package com.coalblend.service.intelligent.impl;

import com.coalblend.entity.CoalQuality;
import com.coalblend.entity.CoalType;
import com.coalblend.entity.Orders;
import com.coalblend.service.intelligent.HumanExperienceBaselineService;
import com.coalblend.service.intelligent.model.HumanExperiencePlan;
import com.coalblend.service.intelligent.model.PlanCoalSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * 人工经验配煤基线服务实现（V1）。法则定义见 {@link HumanExperienceBaselineService}。
 * <p>
 * 为提高基线对真实经验决策的逼真度，V1 同时生成 N=2（60%/40%）与 N=3（50%/30%/20%）
 * 两组候选方案，按"硬约束可行优先 + 方案级综合分更高"原则择优，最终仅输出一个方案。
 */
@Slf4j
@Service
public class HumanExperienceBaselineServiceImpl implements HumanExperienceBaselineService {

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

    @Override
    public HumanExperiencePlan generate(Orders order, List<PlanCoalSnapshot> candidates) {
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

        // Step 2: 同时生成 N=2 与 N=3（候选物料够时）两组候选
        HumanExperiencePlan candidateN2 = buildCandidate(order, scored, 2, RATIOS_N2, maxPrice);
        HumanExperiencePlan candidateN3 = scored.size() >= 3
                ? buildCandidate(order, scored, 3, RATIOS_N3, maxPrice)
                : null;

        // Step 4: 在两份候选间按"可行优先 + 方案级综合分更高"择优，仅输出一份
        HumanExperiencePlan winner = selectBetter(candidateN2, candidateN3);
        winner.setSummary(buildSummary(winner, candidateN2, candidateN3));
        winner.setAlternativeSummary(buildAlternativeSummary(winner, candidateN2, candidateN3));
        return winner;
    }

    /** 构造给定 N 与配比的单份候选方案，并完成 Step 3 硬约束粗校验与方案级综合分计算。 */
    private HumanExperiencePlan buildCandidate(Orders order,
                                               List<ScoredCandidate> sortedScored,
                                               int n,
                                               List<BigDecimal> ratios,
                                               BigDecimal maxPrice) {
        HumanExperiencePlan plan = new HumanExperiencePlan();
        plan.setDemandQuantity(order.getDemandQuantity());
        plan.setSelectedN(n);

        List<ScoredCandidate> picks = sortedScored.subList(0, n);
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
        return plan;
    }

    /** 在 N=2 / N=3 两份候选间择优：可行优先；同档比方案级综合分；再同档比成本更低；再同档取 N=3。 */
    private HumanExperiencePlan selectBetter(HumanExperiencePlan a, HumanExperiencePlan b) {
        if (a == null) return b;
        if (b == null) return a;
        if (a.isHardConstraintsPassed() && !b.isHardConstraintsPassed()) return a;
        if (!a.isHardConstraintsPassed() && b.isHardConstraintsPassed()) return b;

        int scoreCompare = nullSafeCompare(a.getPlanScore(), b.getPlanScore());
        if (scoreCompare > 0) return a;
        if (scoreCompare < 0) return b;

        int costCompare = nullSafeCompare(b.getCostPerTon(), a.getCostPerTon());
        if (costCompare > 0) return a;
        if (costCompare < 0) return b;

        return b;
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

    private String buildSummary(HumanExperiencePlan winner,
                                HumanExperiencePlan candidateN2,
                                HumanExperiencePlan candidateN3) {
        StringBuilder sb = new StringBuilder();
        sb.append("人工经验基线（V1）：");
        sb.append("Step1 按 0.4×热值+0.2×灰+0.2×硫+0.2×价 计算物料综合分；");
        if (candidateN3 != null) {
            sb.append("Step2 同时生成 N=2（60/40）与 N=3（50/30/20）两组候选；");
        } else {
            sb.append("Step2 候选物料不足 3 种，仅生成 N=2（60/40）一组候选；");
        }
        sb.append("Step3 硬约束粗校验：")
                .append(winner.isHardConstraintsPassed()
                        ? "最终方案通过。"
                        : "最终方案超限（" + String.join("；", winner.getViolations()) + "）。");
        sb.append("Step4 按可行优先 + 方案级综合分择优，最终采用 N=").append(winner.getSelectedN())
                .append("（方案综合分 ").append(formatScore(winner.getPlanScore())).append("）。");
        sb.append("不查规则/案例/RAG，不做 Pareto 多目标，仅作对照展示，不参与系统最终推荐。");
        return sb.toString();
    }

    private String buildAlternativeSummary(HumanExperiencePlan winner,
                                           HumanExperiencePlan candidateN2,
                                           HumanExperiencePlan candidateN3) {
        HumanExperiencePlan loser;
        if (candidateN3 == null) {
            return "候选物料不足 3 种，未生成 N=3 备选。";
        }
        loser = (winner == candidateN2) ? candidateN3 : candidateN2;
        StringBuilder sb = new StringBuilder();
        sb.append("被弃选备选：N=").append(loser.getSelectedN())
                .append("，方案综合分 ").append(formatScore(loser.getPlanScore()))
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
}
