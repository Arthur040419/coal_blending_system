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

/**
 * 人工经验配煤基线服务实现（V1）。法则定义见 {@link HumanExperienceBaselineService}。
 */
@Slf4j
@Service
public class HumanExperienceBaselineServiceImpl implements HumanExperienceBaselineService {

    /** Step 1 综合分权重：质量优先（热值 0.4 + 灰 0.2 + 硫 0.2），价格次要（0.2） */
    private static final BigDecimal W_CALORIFIC = new BigDecimal("0.40");
    private static final BigDecimal W_ASH = new BigDecimal("0.20");
    private static final BigDecimal W_SULFUR = new BigDecimal("0.20");
    private static final BigDecimal W_PRICE = new BigDecimal("0.20");

    /** Step 2 经验配比：固定 60% 主煤 + 40% 辅煤 */
    private static final BigDecimal RATIO_PRIMARY = new BigDecimal("0.60");
    private static final BigDecimal RATIO_SECONDARY = new BigDecimal("0.40");

    private static final int SCALE_RATIO = 4;
    private static final int SCALE_QUALITY = 4;
    private static final int SCALE_COST = 2;
    private static final int SCALE_SCORE = 4;

    @Override
    public HumanExperiencePlan generate(Orders order, List<PlanCoalSnapshot> candidates) {
        HumanExperiencePlan plan = new HumanExperiencePlan();
        plan.setDemandQuantity(order == null ? null : order.getDemandQuantity());

        if (order == null || candidates == null || candidates.size() < 2) {
            plan.setGenerated(false);
            plan.setStatus("failed");
            plan.setErrorMessage("候选物料不足 2 种，人工经验基线无法生成对照方案。");
            return plan;
        }

        // Step 1: 计算每个候选物料的经验综合分，按降序排序
        BigDecimal maxPrice = candidates.stream()
                .map(s -> s.getType() == null ? null : s.getType().getPurchasePrice())
                .filter(p -> p != null && p.compareTo(BigDecimal.ZERO) > 0)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ONE);

        List<ScoredCandidate> scored = new ArrayList<>();
        for (PlanCoalSnapshot s : candidates) {
            BigDecimal score = computeExperienceScore(order, s, maxPrice);
            if (score != null) {
                scored.add(new ScoredCandidate(s, score));
            }
        }
        if (scored.size() < 2) {
            plan.setGenerated(false);
            plan.setStatus("failed");
            plan.setErrorMessage("候选物料缺少必要煤质或价格数据，人工经验基线无法计算综合分。");
            return plan;
        }
        scored.sort(Comparator.comparing(ScoredCandidate::score).reversed());

        // Step 2: 固定取前两名按 60/40 配比
        ScoredCandidate primary = scored.get(0);
        ScoredCandidate secondary = scored.get(1);

        HumanExperiencePlan.HumanExperienceItem itemA = toItem(primary, RATIO_PRIMARY);
        HumanExperiencePlan.HumanExperienceItem itemB = toItem(secondary, RATIO_SECONDARY);
        plan.getItems().add(itemA);
        plan.getItems().add(itemB);

        // Step 3: 线性加权计算配合煤指标 + 硬约束粗校验
        plan.setPredictedAsh(weighted(primary, secondary, RATIO_PRIMARY, RATIO_SECONDARY,
                q -> q.getAshContent()));
        plan.setPredictedSulfur(weighted(primary, secondary, RATIO_PRIMARY, RATIO_SECONDARY,
                q -> q.getSulfurContent()));
        plan.setPredictedMoisture(weighted(primary, secondary, RATIO_PRIMARY, RATIO_SECONDARY,
                q -> q.getMoistureContent()));
        plan.setPredictedVolatile(weighted(primary, secondary, RATIO_PRIMARY, RATIO_SECONDARY,
                q -> q.getVolatileContent()));
        plan.setPredictedCalorific(weighted(primary, secondary, RATIO_PRIMARY, RATIO_SECONDARY,
                q -> q.getCalorificValue()));

        BigDecimal pricePerTon = weightedPrice(primary, secondary, RATIO_PRIMARY, RATIO_SECONDARY);
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

        plan.setGenerated(true);
        plan.setSummary(buildSummary(plan, primary, secondary));
        return plan;
    }

    /** Step 1：经验综合分。质量项越优分越高，价格越低分越高。 */
    private BigDecimal computeExperienceScore(Orders order, PlanCoalSnapshot snapshot, BigDecimal maxPrice) {
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

    private BigDecimal weighted(ScoredCandidate a, ScoredCandidate b,
                                BigDecimal ra, BigDecimal rb,
                                java.util.function.Function<CoalQuality, BigDecimal> getter) {
        BigDecimal va = a.snapshot().getQuality() == null ? null : getter.apply(a.snapshot().getQuality());
        BigDecimal vb = b.snapshot().getQuality() == null ? null : getter.apply(b.snapshot().getQuality());
        if (va == null || vb == null) {
            return null;
        }
        return va.multiply(ra).add(vb.multiply(rb)).setScale(SCALE_QUALITY, RoundingMode.HALF_UP);
    }

    private BigDecimal weightedPrice(ScoredCandidate a, ScoredCandidate b,
                                     BigDecimal ra, BigDecimal rb) {
        BigDecimal pa = a.snapshot().getType() == null ? null : a.snapshot().getType().getPurchasePrice();
        BigDecimal pb = b.snapshot().getType() == null ? null : b.snapshot().getType().getPurchasePrice();
        if (pa == null || pb == null) {
            return null;
        }
        return pa.multiply(ra).add(pb.multiply(rb)).setScale(SCALE_COST, RoundingMode.HALF_UP);
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

    private String buildSummary(HumanExperiencePlan plan, ScoredCandidate primary, ScoredCandidate secondary) {
        String primaryName = primary.snapshot().getType() == null ? "" : primary.snapshot().getType().getCoalName();
        String secondaryName = secondary.snapshot().getType() == null ? "" : secondary.snapshot().getType().getCoalName();
        StringBuilder sb = new StringBuilder();
        sb.append("人工经验基线（V1）：");
        sb.append("Step1 按 0.4×热值+0.2×灰+0.2×硫+0.2×价 计算综合分；");
        sb.append("Step2 取前两名 ").append(primaryName).append("（综合分 ").append(primary.score().toPlainString()).append("）")
                .append("、").append(secondaryName).append("（综合分 ").append(secondary.score().toPlainString()).append("），")
                .append("按 60% / 40% 配比；");
        sb.append("Step3 硬约束粗校验：")
                .append(plan.isHardConstraintsPassed() ? "通过。" : "超限（" + String.join("；", plan.getViolations()) + "）。");
        sb.append("不查规则/案例/RAG，不做 Pareto 多目标，仅作对照展示，不参与系统最终推荐。");
        return sb.toString();
    }

    /** 局部数据载体：候选物料 + 经验综合分。 */
    private record ScoredCandidate(PlanCoalSnapshot snapshot, BigDecimal score) {
    }
}
