package com.coalblend.service.intelligent.impl;

import com.coalblend.entity.CoalQuality;
import com.coalblend.entity.CoalType;
import com.coalblend.entity.Orders;
import com.coalblend.enums.PlanDecisionStatus;
import com.coalblend.enums.ScoreStrategyType;
import com.coalblend.service.intelligent.HumanExperienceBaselineService;
import com.coalblend.service.intelligent.ParetoRankService;
import com.coalblend.service.intelligent.PlanDecisionService;
import com.coalblend.service.intelligent.PlanScoreService;
import com.coalblend.service.intelligent.model.BlendGenerationRuntimeConfig;
import com.coalblend.service.intelligent.model.EvaluatedPlanDraft;
import com.coalblend.service.intelligent.model.HumanExperiencePlan;
import com.coalblend.service.intelligent.model.PlanCoalSnapshot;
import com.coalblend.service.intelligent.model.ScoreDetail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 人工经验配煤基线服务实现（V1）。法则定义见 {@link HumanExperienceBaselineService}。
 * <p>
 * V1 按固定人工经验配比模板生成 N=2（60%/40%）与 N=3（50%/30%/20%）候选方案，
 * 复用系统决策校验后按"可执行优先 + 综合评分更高 + 吨煤成本更低"原则择优，最终仅输出一个方案。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HumanExperienceBaselineServiceImpl implements HumanExperienceBaselineService {

    private final PlanScoreService planScoreService;
    private final PlanDecisionService planDecisionService;
    private final ParetoRankService paretoRankService;

    /** Step 1 综合分权重：质量优先（热值 0.4 + 灰 0.2 + 硫 0.2），价格次要（0.2） */
    private static final BigDecimal W_CALORIFIC = new BigDecimal("0.40");
    private static final BigDecimal W_ASH = new BigDecimal("0.20");
    private static final BigDecimal W_SULFUR = new BigDecimal("0.20");
    private static final BigDecimal W_PRICE = new BigDecimal("0.20");

    /** Step 2 经验配比：N=2 → 60% / 40%；N=3 → 50% / 30% / 20% */
    private static final List<BigDecimal> RATIOS_N2 = List.of(
            new BigDecimal("0.60"),
            new BigDecimal("0.40"));
    private static final List<BigDecimal> RATIOS_N3 = List.of(
            new BigDecimal("0.50"),
            new BigDecimal("0.30"),
            new BigDecimal("0.20"));

    private static final int SCALE_RATIO = 4;
    private static final int SCALE_QUALITY = 4;
    private static final int SCALE_COST = 2;
    private static final int SCALE_SCORE = 4;
    private static final int MAX_EXPERIENCE_POOL_SIZE = 4;

    @Override
    public HumanExperiencePlan generate(Orders order, List<PlanCoalSnapshot> candidates,
                                        BlendGenerationRuntimeConfig runtimeConfig) {
        HumanExperiencePlan failurePlan = new HumanExperiencePlan();
        failurePlan.setDemandQuantity(order == null ? null : order.getDemandQuantity());

        if (order == null || candidates == null || candidates.size() < 2) {
            failurePlan.setGenerated(false);
            failurePlan.setStatus("failed");
            failurePlan.setErrorMessage("候选物料不足 2 种，人工经验基线无法生成对照方案。");
            return failurePlan;
        }

        // Step 1: 计算每个候选物料的经验综合分，按降序排序
        BigDecimal maxPrice = candidates.stream()
                .map(s -> s.getType() == null ? null : s.getType().getPurchasePrice())
                .filter(p -> p != null && p.compareTo(BigDecimal.ZERO) > 0)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ONE);

        List<ScoredCandidate> scored = new ArrayList<>();
        for (PlanCoalSnapshot s : candidates) {
            BigDecimal score = computeMaterialScore(order, s, maxPrice);
            if (score != null) {
                scored.add(new ScoredCandidate(s, score));
            }
        }
        if (scored.size() < 2) {
            failurePlan.setGenerated(false);
            failurePlan.setStatus("failed");
            failurePlan.setErrorMessage("候选物料缺少必要煤质或价格数据，人工经验基线无法计算综合分。");
            return failurePlan;
        }
        scored.sort(Comparator.comparing(ScoredCandidate::score).reversed());

        // Step 2: 仅在经验排序前 4 个煤种内尝试固定人工配比，避免人工基线退化成全量搜索
        List<ScoredCandidate> experiencePool = scored.subList(0, Math.min(MAX_EXPERIENCE_POOL_SIZE, scored.size()));
        List<BaselineCandidate> baselineCandidates = buildBaselineCandidates(order, experiencePool, maxPrice, runtimeConfig);
        List<EvaluatedPlanDraft> comparableDrafts = baselineCandidates.stream()
                .map(BaselineCandidate::draft)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        planDecisionService.decide(order, comparableDrafts, runtimeConfig);
        paretoRankService.rank(order, comparableDrafts);
        baselineCandidates.forEach(this::syncComparableMetrics);

        // Step 4: 可执行优先；同状态下按 Pareto、综合评分和成本择优，仅输出一份
        BaselineCandidate winnerCandidate = baselineCandidates.stream()
                .min(this::compareBaselineCandidates)
                .orElse(null);
        if (winnerCandidate == null) {
            failurePlan.setGenerated(false);
            failurePlan.setStatus("failed");
            failurePlan.setErrorMessage("人工经验候选生成失败。");
            return failurePlan;
        }
        HumanExperiencePlan winner = winnerCandidate.plan();
        winner.setSummary(buildSummary(winner, baselineCandidates, experiencePool.size()));
        winner.setAlternativeSummary(buildAlternativeSummary(winner, baselineCandidates));
        return winner;
    }

    private List<BaselineCandidate> buildBaselineCandidates(Orders order,
                                                            List<ScoredCandidate> sortedScored,
                                                            BigDecimal maxPrice,
                                                            BlendGenerationRuntimeConfig runtimeConfig) {
        List<BaselineCandidate> candidates = new ArrayList<>();
        int size = sortedScored.size();
        for (int i = 0; i < size - 1; i++) {
            for (int j = i + 1; j < size; j++) {
                candidates.add(buildCandidate(order, List.of(sortedScored.get(i), sortedScored.get(j)),
                        2, RATIOS_N2, maxPrice, runtimeConfig));
            }
        }
        if (size >= 3) {
            for (int i = 0; i < size - 2; i++) {
                for (int j = i + 1; j < size - 1; j++) {
                    for (int k = j + 1; k < size; k++) {
                        candidates.add(buildCandidate(order,
                                List.of(sortedScored.get(i), sortedScored.get(j), sortedScored.get(k)),
                                3, RATIOS_N3, maxPrice, runtimeConfig));
                    }
                }
            }
        }
        return candidates;
    }

    /** 构造给定 N 与配比的单份候选方案，并完成 Step 3 硬约束粗校验与方案级综合分计算。 */
    private BaselineCandidate buildCandidate(Orders order,
                                             List<ScoredCandidate> picks,
                                             int n,
                                             List<BigDecimal> ratios,
                                             BigDecimal maxPrice,
                                             BlendGenerationRuntimeConfig runtimeConfig) {
        HumanExperiencePlan plan = new HumanExperiencePlan();
        plan.setDemandQuantity(order.getDemandQuantity());
        plan.setSelectedN(n);

        for (int i = 0; i < n; i++) {
            plan.getItems().add(toItem(picks.get(i), ratios.get(i)));
        }

        plan.setPredictedAsh(weighted(picks, ratios, q -> q.getAshContent()));
        plan.setPredictedSulfur(weighted(picks, ratios, q -> q.getSulfurContent()));
        plan.setPredictedMoisture(weighted(picks, ratios, q -> q.getMoistureContent()));
        plan.setPredictedVolatile(weighted(picks, ratios, q -> q.getVolatileContent()));
        plan.setPredictedCalorific(weighted(picks, ratios, q -> q.getCalorificValue()));

        BigDecimal pricePerTon = weightedPrice(picks, ratios);
        plan.setCostPerTon(pricePerTon);
        if (pricePerTon != null && order.getDemandQuantity() != null) {
            plan.setTotalCost(pricePerTon.multiply(order.getDemandQuantity())
                    .setScale(SCALE_COST, RoundingMode.HALF_UP));
        }

        List<String> violations = checkHardConstraints(order, plan);
        if (violations.isEmpty()) {
            plan.setHardConstraintsPassed(true);
            plan.setStatus("feasible");
        } else {
            plan.setHardConstraintsPassed(false);
            plan.setStatus("infeasible");
            plan.getViolations().addAll(violations);
        }

        plan.setPlanScore(computePlanScore(plan, order, maxPrice));
        plan.setGenerated(true);

        List<PlanCoalSnapshot> snapshots = picks.stream()
                .map(ScoredCandidate::snapshot)
                .collect(Collectors.toList());
        EvaluatedPlanDraft draft = planScoreService.evaluate(order, snapshots, ratios,
                resolveScoreStrategy(runtimeConfig), runtimeConfig);
        draft.setCandidateSource("human");
        return new BaselineCandidate(plan, draft);
    }

    private int compareBaselineCandidates(BaselineCandidate left, BaselineCandidate right) {
        HumanExperiencePlan a = left == null ? null : left.plan();
        HumanExperiencePlan b = right == null ? null : right.plan();
        if (a == null && b == null) return 0;
        if (a == null) return 1;
        if (b == null) return -1;

        int statusCompare = Integer.compare(decisionPriority(a), decisionPriority(b));
        if (statusCompare != 0) return statusCompare;

        int paretoCompare = Integer.compare(nullAsLarge(a.getParetoRank()), nullAsLarge(b.getParetoRank()));
        if (paretoCompare != 0) return paretoCompare;

        int overallCompare = nullSafeCompare(b.getOverallScore(), a.getOverallScore());
        if (overallCompare != 0) return overallCompare;

        int planScoreCompare = nullSafeCompare(b.getPlanScore(), a.getPlanScore());
        if (planScoreCompare != 0) return planScoreCompare;

        int costCompare = nullSafeCompare(b.getCostPerTon(), a.getCostPerTon());
        if (costCompare > 0) return -1;
        if (costCompare < 0) return 1;

        return Integer.compare(nullAsLarge(a.getSelectedN()), nullAsLarge(b.getSelectedN()));
    }

    private int decisionPriority(HumanExperiencePlan plan) {
        String status = plan == null ? null : plan.getDecisionStatus();
        if (PlanDecisionStatus.FEASIBLE.name().equals(status)) {
            return PlanDecisionStatus.FEASIBLE.getPriority();
        }
        if (PlanDecisionStatus.RISKY.name().equals(status)) {
            return PlanDecisionStatus.RISKY.getPriority();
        }
        if (PlanDecisionStatus.INFEASIBLE.name().equals(status)) {
            return PlanDecisionStatus.INFEASIBLE.getPriority();
        }
        return plan != null && plan.isHardConstraintsPassed()
                ? PlanDecisionStatus.RISKY.getPriority()
                : PlanDecisionStatus.INFEASIBLE.getPriority();
    }

    private int nullAsLarge(Integer value) {
        return value == null ? Integer.MAX_VALUE : value;
    }

    private ScoreStrategyType resolveScoreStrategy(BlendGenerationRuntimeConfig runtimeConfig) {
        return runtimeConfig == null || runtimeConfig.getScoreStrategy() == null
                ? ScoreStrategyType.BALANCED
                : runtimeConfig.getScoreStrategy();
    }

    private void syncComparableMetrics(BaselineCandidate candidate) {
        if (candidate == null || candidate.plan() == null || candidate.draft() == null) {
            return;
        }
        HumanExperiencePlan plan = candidate.plan();
        EvaluatedPlanDraft draft = candidate.draft();
        ScoreDetail score = draft.getScoreDetail();
        if (score != null) {
            plan.setQualityScore(score.getQualityScore());
            plan.setCostScore(score.getCostScore());
            plan.setStabilityScore(score.getStabilityScore());
            plan.setOverallScore(score.getOverallScore());
        }
        plan.setDecisionStatus(draft.getDecisionStatus());
        plan.setDecisionStatusLabel(draft.getDecisionStatusLabel());
        plan.setParetoRank(draft.getParetoRank());
        plan.setDominatedCount(draft.getDominatedCount());
        plan.setDominatesCount(draft.getDominatesCount());
        plan.setObjectiveCostPerTon(draft.getObjectiveCostPerTon());
        plan.setObjectiveQualityDeviation(draft.getObjectiveQualityDeviation());
        plan.setObjectiveExecutionRisk(draft.getObjectiveExecutionRisk());
        plan.setMainProblem(mainProblemText(draft));
    }

    private String mainProblemText(EvaluatedPlanDraft draft) {
        if (draft == null || draft.getProblemItems() == null || draft.getProblemItems().isEmpty()) {
            return "—";
        }
        String text = draft.getProblemItems().stream()
                .map(item -> StringUtils.hasText(item.getTypeLabel()) ? item.getTypeLabel() : item.getMessage())
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.joining("；"));
        return StringUtils.hasText(text) ? text : "—";
    }

    /** 候选物料级综合分（Step 1 公式）。 */
    private BigDecimal computeMaterialScore(Orders order, PlanCoalSnapshot snapshot, BigDecimal maxPrice) {
        if (snapshot == null || snapshot.getQuality() == null || snapshot.getType() == null) {
            return null;
        }
        CoalQuality q = snapshot.getQuality();
        CoalType t = snapshot.getType();
        if (t.getPurchasePrice() == null) {
            return null;
        }

        BigDecimal calorificScore = positiveDeviationScore(q.getCalorificValue(), order.getTargetCalorific(), true);
        BigDecimal ashScore = positiveDeviationScore(q.getAshContent(), order.getTargetAsh(), false);
        BigDecimal sulfurScore = positiveDeviationScore(q.getSulfurContent(), order.getTargetSulfur(), false);
        BigDecimal priceScore = priceScore(t.getPurchasePrice(), maxPrice);

        return weightedSum(calorificScore, ashScore, sulfurScore, priceScore);
    }

    /**
     * 方案级综合分：与候选物料级综合分使用相同权重，但代入加权后的方案指标。
     * 用于在 N=2 / N=3 两份候选之间择优。
     */
    private BigDecimal computePlanScore(HumanExperiencePlan plan, Orders order, BigDecimal maxPrice) {
        BigDecimal calorificScore = positiveDeviationScore(plan.getPredictedCalorific(), order.getTargetCalorific(), true);
        BigDecimal ashScore = positiveDeviationScore(plan.getPredictedAsh(), order.getTargetAsh(), false);
        BigDecimal sulfurScore = positiveDeviationScore(plan.getPredictedSulfur(), order.getTargetSulfur(), false);
        BigDecimal priceScore = priceScore(plan.getCostPerTon(), maxPrice);
        return weightedSum(calorificScore, ashScore, sulfurScore, priceScore);
    }

    private BigDecimal weightedSum(BigDecimal calorificScore, BigDecimal ashScore,
                                   BigDecimal sulfurScore, BigDecimal priceScore) {
        if (calorificScore == null || ashScore == null || sulfurScore == null || priceScore == null) {
            return null;
        }
        return calorificScore.multiply(W_CALORIFIC)
                .add(ashScore.multiply(W_ASH))
                .add(sulfurScore.multiply(W_SULFUR))
                .add(priceScore.multiply(W_PRICE))
                .setScale(SCALE_SCORE, RoundingMode.HALF_UP);
    }

    /**
     * 质量项偏离得分：与订单门限的相对偏离（百分制）。
     * <p>热值是下限（实际 ≥ 目标更优），灰/硫是上限（实际 ≤ 目标更优）。
     */
    private BigDecimal positiveDeviationScore(BigDecimal actual, BigDecimal target, boolean lowerBound) {
        if (actual == null || target == null || target.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        BigDecimal diff = lowerBound
                ? actual.subtract(target)
                : target.subtract(actual);
        return diff.multiply(new BigDecimal("100"))
                .divide(target, SCALE_SCORE, RoundingMode.HALF_UP);
    }

    private BigDecimal priceScore(BigDecimal price, BigDecimal maxPrice) {
        if (price == null || maxPrice == null || maxPrice.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return maxPrice.subtract(price).multiply(new BigDecimal("100"))
                .divide(maxPrice, SCALE_SCORE, RoundingMode.HALF_UP);
    }

    private HumanExperiencePlan.HumanExperienceItem toItem(ScoredCandidate sc, BigDecimal ratio) {
        HumanExperiencePlan.HumanExperienceItem item = new HumanExperiencePlan.HumanExperienceItem();
        PlanCoalSnapshot s = sc.snapshot();
        CoalType t = s.getType();
        CoalQuality q = s.getQuality();
        item.setCoalId(s.getCoalId());
        item.setCoalCode(t == null ? null : t.getCoalCode());
        item.setCoalName(t == null ? null : t.getCoalName());
        item.setExperienceScore(sc.score());
        item.setRatio(ratio.setScale(SCALE_RATIO, RoundingMode.HALF_UP));
        item.setPurchasePrice(t == null ? null : t.getPurchasePrice());
        item.setAsh(q == null ? null : q.getAshContent());
        item.setSulfur(q == null ? null : q.getSulfurContent());
        item.setMoisture(q == null ? null : q.getMoistureContent());
        item.setVolatileMatter(q == null ? null : q.getVolatileContent());
        item.setCalorific(q == null ? null : q.getCalorificValue());
        return item;
    }

    /** 通用：对若干候选按对应配比线性加权求煤质指标。任一空值都回退为 null。 */
    private BigDecimal weighted(List<ScoredCandidate> picks, List<BigDecimal> ratios,
                                Function<CoalQuality, BigDecimal> getter) {
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < picks.size(); i++) {
            CoalQuality quality = picks.get(i).snapshot().getQuality();
            if (quality == null) return null;
            BigDecimal value = getter.apply(quality);
            if (value == null) return null;
            sum = sum.add(value.multiply(ratios.get(i)));
        }
        return sum.setScale(SCALE_QUALITY, RoundingMode.HALF_UP);
    }

    private BigDecimal weightedPrice(List<ScoredCandidate> picks, List<BigDecimal> ratios) {
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < picks.size(); i++) {
            CoalType type = picks.get(i).snapshot().getType();
            if (type == null || type.getPurchasePrice() == null) return null;
            sum = sum.add(type.getPurchasePrice().multiply(ratios.get(i)));
        }
        return sum.setScale(SCALE_COST, RoundingMode.HALF_UP);
    }

    /** Step 3：硬约束粗校验。仅检查灰/硫/水（≤ 上限）和热值（≥ 下限）。 */
    private List<String> checkHardConstraints(Orders order, HumanExperiencePlan plan) {
        List<String> violations = new ArrayList<>();
        addUpperViolation(violations, "灰分", plan.getPredictedAsh(), order.getTargetAsh());
        addUpperViolation(violations, "硫分", plan.getPredictedSulfur(), order.getTargetSulfur());
        addUpperViolation(violations, "水分", plan.getPredictedMoisture(), order.getTargetMoisture());
        addLowerViolation(violations, "热值", plan.getPredictedCalorific(), order.getTargetCalorific());
        return violations;
    }

    private void addUpperViolation(List<String> violations, String label,
                                   BigDecimal actual, BigDecimal target) {
        if (actual != null && target != null && actual.compareTo(target) > 0) {
            violations.add(String.format(Locale.ROOT,
                    "%s 加权值 %s 超过订单上限 %s", label, actual.toPlainString(), target.toPlainString()));
        }
    }

    private void addLowerViolation(List<String> violations, String label,
                                   BigDecimal actual, BigDecimal target) {
        if (actual != null && target != null && actual.compareTo(target) < 0) {
            violations.add(String.format(Locale.ROOT,
                    "%s 加权值 %s 低于订单下限 %s", label, actual.toPlainString(), target.toPlainString()));
        }
    }

    private String buildSummary(HumanExperiencePlan winner, List<BaselineCandidate> candidates, int poolSize) {
        StringBuilder sb = new StringBuilder();
        sb.append("人工经验基线（V1）：");
        sb.append("Step1 按 0.4×热值+0.2×灰+0.2×硫+0.2×价 计算物料综合分；");
        sb.append("Step2 仅在经验排序前 ").append(poolSize)
                .append(" 个煤种内生成 N=2（60/40）与 N=3（50/30/20）人工经验候选共 ")
                .append(candidates == null ? 0 : candidates.size()).append(" 组；");
        sb.append("Step3 硬约束粗校验：")
                .append(winner.isHardConstraintsPassed()
                        ? "最终方案通过。"
                        : "最终方案超限（" + String.join("；", winner.getViolations()) + "）。");
        sb.append("Step4 按可执行优先 + Pareto等级 + 系统综合评分 + 吨煤成本择优，最终采用 N=")
                .append(winner.getSelectedN())
                .append("（决策状态 ").append(StringUtils.hasText(winner.getDecisionStatusLabel())
                        ? winner.getDecisionStatusLabel() : "-")
                .append("，综合评分 ").append(formatScore(winner.getOverallScore())).append("）。");
        sb.append("不查规则/案例/RAG，仅作对照展示，不参与系统最终推荐。");
        return sb.toString();
    }

    private String buildAlternativeSummary(HumanExperiencePlan winner, List<BaselineCandidate> candidates) {
        if (candidates == null || candidates.size() <= 1) {
            return "未生成其他人工经验备选。";
        }
        HumanExperiencePlan loser = candidates.stream()
                .map(BaselineCandidate::plan)
                .filter(plan -> plan != winner)
                .min((a, b) -> compareBaselineCandidates(new BaselineCandidate(a, null), new BaselineCandidate(b, null)))
                .orElse(null);
        if (loser == null) {
            return "未生成其他人工经验备选。";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("被弃选备选：N=").append(loser.getSelectedN())
                .append("，决策状态 ").append(StringUtils.hasText(loser.getDecisionStatusLabel())
                        ? loser.getDecisionStatusLabel() : "-")
                .append("，综合评分 ").append(formatScore(loser.getOverallScore()))
                .append("，吨煤成本 ").append(loser.getCostPerTon() == null ? "-" : loser.getCostPerTon().toPlainString()).append(" 元/t，")
                .append(loser.isHardConstraintsPassed() ? "硬约束通过。" : "硬约束超限。");
        return sb.toString();
    }

    private String formatScore(BigDecimal score) {
        return score == null ? "-" : score.toPlainString();
    }

    private int nullSafeCompare(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        return a.compareTo(b);
    }

    /** 局部数据载体：候选物料 + 经验综合分。 */
    private record ScoredCandidate(PlanCoalSnapshot snapshot, BigDecimal score) {
    }

    /** 局部数据载体：人工经验方案 + 系统同口径评价草稿。 */
    private record BaselineCandidate(HumanExperiencePlan plan, EvaluatedPlanDraft draft) {
    }
}
