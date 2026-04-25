package com.coalblend.service.intelligent.impl;

import com.coalblend.entity.BlendPlanDetail;
import com.coalblend.entity.CoalQuality;
import com.coalblend.entity.CoalType;
import com.coalblend.entity.Inventory;
import com.coalblend.entity.Orders;
import com.coalblend.service.intelligent.PlanScoreService;
import com.coalblend.service.intelligent.model.ConstraintResult;
import com.coalblend.service.intelligent.model.EvaluatedPlanDraft;
import com.coalblend.service.intelligent.model.PlanCoalSnapshot;
import com.coalblend.service.intelligent.model.ScoreDetail;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PlanScoreServiceImpl implements PlanScoreService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final BigDecimal STOCK_WARN = new BigDecimal("3000");
    private static final BigDecimal STOCK_CRITICAL = new BigDecimal("1000");

    @Override
    public EvaluatedPlanDraft evaluate(Orders order, List<PlanCoalSnapshot> snapshots, List<BigDecimal> ratios) {
        BigDecimal wAsh = BigDecimal.ZERO;
        BigDecimal wS = BigDecimal.ZERO;
        BigDecimal wM = BigDecimal.ZERO;
        BigDecimal wV = BigDecimal.ZERO;
        BigDecimal wCal = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;

        BigDecimal cheapestUnitPrice = snapshots.stream()
                .map(PlanCoalSnapshot::getType)
                .map(CoalType::getPurchasePrice)
                .filter(v -> v != null && v.compareTo(BigDecimal.ZERO) > 0)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        BigDecimal mostExpensiveUnitPrice = snapshots.stream()
                .map(PlanCoalSnapshot::getType)
                .map(CoalType::getPurchasePrice)
                .filter(v -> v != null && v.compareTo(BigDecimal.ZERO) > 0)
                .max(BigDecimal::compareTo)
                .orElse(cheapestUnitPrice);

        ConstraintResult constraint = new ConstraintResult();
        List<BlendPlanDetail> details = new ArrayList<>();

        for (int i = 0; i < snapshots.size(); i++) {
            PlanCoalSnapshot snapshot = snapshots.get(i);
            BigDecimal r = ratios.get(i);
            CoalQuality q = snapshot.getQuality();
            CoalType t = snapshot.getType();
            Inventory inv = snapshot.getInventory();
            BigDecimal useQty = order.getDemandQuantity().multiply(r).setScale(2, RoundingMode.HALF_UP);
            BigDecimal price = t.getPurchasePrice() == null ? BigDecimal.ZERO : t.getPurchasePrice();
            totalCost = totalCost.add(useQty.multiply(price));
            wAsh = wAsh.add(nzMetric(q.getAshContent()).multiply(r));
            wS = wS.add(nzMetric(q.getSulfurContent()).multiply(r));
            wM = wM.add(nzMetric(q.getMoistureContent()).multiply(r));
            wV = wV.add(nzMetric(q.getVolatileContent()).multiply(r));
            wCal = wCal.add(nzMetric(q.getCalorificValue()).multiply(r));

            BigDecimal available = inv.getAvailableQuantity() == null ? BigDecimal.ZERO : inv.getAvailableQuantity();
            BigDecimal remain = available.subtract(useQty);
            if (useQty.compareTo(available) > 0) {
                constraint.addViolation(t.getCoalName() + "用量超过可用库存");
            } else if (remain.compareTo(STOCK_CRITICAL) < 0) {
                constraint.addWarning(t.getCoalName() + "执行后余量低于1000吨");
            } else if (available.compareTo(STOCK_WARN) < 0) {
                constraint.addWarning(t.getCoalName() + "当前库存低于3000吨");
            }

            BlendPlanDetail d = new BlendPlanDetail();
            d.setCoalId(snapshot.getCoalId());
            d.setProductBatchId(snapshot.getProductBatchId());
            d.setProductBatchNo(snapshot.getProductBatchNo());
            d.setQualitySnapshotJson(snapshot.getQualitySnapshotJson());
            d.setBlendRatio(r);
            d.setUseQuantity(useQty);
            d.setUnitCost(price);
            String materialName = snapshot.getProductBatchNo() == null ? t.getCoalName()
                    : (snapshot.getProductBatchName() + "（" + snapshot.getProductBatchNo() + "）");
            d.setRemark(materialName + "；库存" + available.setScale(2, RoundingMode.HALF_UP).toPlainString()
                    + "吨，预计剩余" + remain.setScale(2, RoundingMode.HALF_UP).toPlainString() + "吨");
            details.add(d);
        }

        BigDecimal pAsh = wAsh.setScale(2, RoundingMode.HALF_UP);
        BigDecimal pS = wS.setScale(2, RoundingMode.HALF_UP);
        BigDecimal pM = wM.setScale(2, RoundingMode.HALF_UP);
        BigDecimal pV = wV.setScale(2, RoundingMode.HALF_UP);
        BigDecimal pCal = wCal.setScale(2, RoundingMode.HALF_UP);
        fillPredictedMetrics(details, pAsh, pS, pM, pV, pCal);
        fillMetricConstraints(order, constraint, pAsh, pS, pM, pCal);

        ScoreDetail score = buildScoreDetail(order, details, snapshots, totalCost, cheapestUnitPrice,
                mostExpensiveUnitPrice, pAsh, pS, pM, pCal);
        String explanation = buildPlanExplanation(snapshots, details, pAsh, pS, pCal, score, constraint.isFeasible());

        EvaluatedPlanDraft draft = new EvaluatedPlanDraft();
        draft.setCoalIds(snapshots.stream().map(PlanCoalSnapshot::getCoalId).collect(Collectors.toList()));
        draft.setRatios(ratios);
        draft.setDetails(details);
        draft.setTotalCost(totalCost.setScale(2, RoundingMode.HALF_UP));
        draft.setConstraintResult(constraint);
        draft.setScoreDetail(score);
        draft.setExplanation(explanation);
        draft.setRiskTip(buildRiskTip(constraint));
        return draft;
    }

    private void fillPredictedMetrics(List<BlendPlanDetail> details, BigDecimal pAsh, BigDecimal pS,
                                      BigDecimal pM, BigDecimal pV, BigDecimal pCal) {
        for (BlendPlanDetail d : details) {
            d.setPredictedAsh(pAsh);
            d.setPredictedSulfur(pS);
            d.setPredictedMoisture(pM);
            d.setPredictedVolatile(pV);
            d.setPredictedCalorific(pCal);
        }
    }

    private void fillMetricConstraints(Orders order, ConstraintResult constraint, BigDecimal ash, BigDecimal s,
                                       BigDecimal m, BigDecimal cal) {
        constraint.setPredictedAsh(ash);
        constraint.setPredictedSulfur(s);
        constraint.setPredictedMoisture(m);
        constraint.setPredictedCalorific(cal);
        if (!passesUpperConstraint(order.getTargetSulfur(), s)) {
            constraint.addViolation("预测硫分" + s + "%超过订单上限" + order.getTargetSulfur() + "%");
        }
        if (!passesUpperConstraint(order.getTargetAsh(), ash)) {
            constraint.addViolation("预测灰分" + ash + "%超过订单上限" + order.getTargetAsh() + "%");
        }
        if (!passesUpperConstraint(order.getTargetMoisture(), m)) {
            constraint.addViolation("预测水分" + m + "%超过订单上限" + order.getTargetMoisture() + "%");
        }
        if (!passesLowerConstraint(order.getTargetCalorific(), cal)) {
            constraint.addViolation("预测发热量" + cal + "低于订单下限" + order.getTargetCalorific());
        }
    }

    private ScoreDetail buildScoreDetail(Orders order, List<BlendPlanDetail> details, List<PlanCoalSnapshot> snapshots,
                                         BigDecimal totalCost, BigDecimal cheapestUnitPrice,
                                         BigDecimal mostExpensiveUnitPrice, BigDecimal ash, BigDecimal s,
                                         BigDecimal m, BigDecimal cal) {
        BigDecimal qualityScore = scoreQuality(order, ash, s, m, cal);
        BigDecimal costScore = scoreCost(totalCost, order.getDemandQuantity(), cheapestUnitPrice, mostExpensiveUnitPrice);
        BigDecimal stabilityScore = scoreStability(details, snapshots);
        BigDecimal overall = qualityScore.multiply(new BigDecimal("0.50"))
                .add(costScore.multiply(new BigDecimal("0.20")))
                .add(stabilityScore.multiply(new BigDecimal("0.30")))
                .setScale(2, RoundingMode.HALF_UP);

        ScoreDetail detail = new ScoreDetail();
        detail.setQualityScore(qualityScore);
        detail.setCostScore(costScore);
        detail.setStabilityScore(stabilityScore);
        detail.setOverallScore(overall);
        detail.setQualityReason("根据灰分、硫分、水分和发热量相对订单目标的偏差计算");
        detail.setCostReason("根据候选煤种价格区间内的吨煤成本位置计算");
        detail.setStabilityReason("根据库存覆盖倍数、执行后库存余量和组合煤种数量计算");
        detail.setOverallReason("综合评分=质量50%+成本20%+库存稳定性30%");
        return detail;
    }

    private BigDecimal scoreQuality(Orders order, BigDecimal ash, BigDecimal s, BigDecimal m, BigDecimal cal) {
        BigDecimal score = HUNDRED;
        score = score.subtract(penaltyUpper(ash, order.getTargetAsh(), new BigDecimal("18")));
        score = score.subtract(penaltyUpper(s, order.getTargetSulfur(), new BigDecimal("30")));
        score = score.subtract(penaltyUpper(m, order.getTargetMoisture(), new BigDecimal("12")));
        score = score.subtract(penaltyLower(cal, order.getTargetCalorific(), new BigDecimal("25")));
        score = score.add(rewardUpper(ash, order.getTargetAsh(), new BigDecimal("3")));
        score = score.add(rewardUpper(s, order.getTargetSulfur(), new BigDecimal("4")));
        score = score.add(rewardLower(cal, order.getTargetCalorific(), new BigDecimal("4")));
        return clampScore(score);
    }

    private BigDecimal scoreCost(BigDecimal totalCost, BigDecimal demand, BigDecimal minPrice, BigDecimal maxPrice) {
        if (demand == null || demand.compareTo(BigDecimal.ZERO) <= 0) {
            return new BigDecimal("70.00");
        }
        BigDecimal unit = totalCost.divide(demand, 4, RoundingMode.HALF_UP);
        if (minPrice == null || maxPrice == null || maxPrice.compareTo(minPrice) <= 0) {
            return new BigDecimal("80.00");
        }
        BigDecimal normalized = maxPrice.subtract(unit)
                .divide(maxPrice.subtract(minPrice), 4, RoundingMode.HALF_UP);
        BigDecimal score = new BigDecimal("40").add(normalized.multiply(new BigDecimal("60")));
        return clampScore(score);
    }

    private BigDecimal scoreStability(List<BlendPlanDetail> details, List<PlanCoalSnapshot> snapshots) {
        if (details.isEmpty()) {
            return new BigDecimal("60.00");
        }
        Map<Long, PlanCoalSnapshot> snapshotMap = snapshots.stream()
                .collect(Collectors.toMap(PlanCoalSnapshot::getCoalId, s -> s, (a, b) -> a));
        BigDecimal coverageTotal = BigDecimal.ZERO;
        int tightCount = 0;
        for (BlendPlanDetail d : details) {
            PlanCoalSnapshot snapshot = snapshotMap.get(d.getCoalId());
            if (snapshot == null || snapshot.getInventory().getAvailableQuantity() == null || d.getUseQuantity() == null
                    || d.getUseQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal available = snapshot.getInventory().getAvailableQuantity();
            BigDecimal coverage = available.divide(d.getUseQuantity(), 4, RoundingMode.HALF_UP);
            coverageTotal = coverageTotal.add(coverage.min(new BigDecimal("2.5")));
            BigDecimal remain = available.subtract(d.getUseQuantity());
            if (remain.compareTo(STOCK_WARN) < 0) {
                tightCount++;
            }
        }
        BigDecimal avgCoverage = coverageTotal.divide(new BigDecimal(details.size()), 4, RoundingMode.HALF_UP);
        BigDecimal score = new BigDecimal("55");
        score = score.add(avgCoverage.min(new BigDecimal("2")).subtract(ONE).max(BigDecimal.ZERO).multiply(new BigDecimal("22")));
        score = score.add(details.size() >= 3 ? new BigDecimal("10") : new BigDecimal("5"));
        score = score.subtract(new BigDecimal(tightCount).multiply(new BigDecimal("8")));
        return clampScore(score);
    }

    private String buildPlanExplanation(List<PlanCoalSnapshot> snapshots, List<BlendPlanDetail> details,
                                        BigDecimal ash, BigDecimal sulfur, BigDecimal calorific,
                                        ScoreDetail score, boolean feasible) {
        String mix = details.stream()
                .map(d -> {
                    PlanCoalSnapshot snapshot = snapshots.stream()
                            .filter(s -> s.getCoalId().equals(d.getCoalId()))
                            .findFirst()
                            .orElse(null);
                    String name = snapshot == null ? ("煤种" + d.getCoalId()) : snapshot.getType().getCoalName();
                    BigDecimal pct = d.getBlendRatio().multiply(HUNDRED).setScale(0, RoundingMode.HALF_UP);
                    return name + pct.toPlainString() + "%";
                })
                .collect(Collectors.joining(" + "));
        String status = feasible ? "满足当前主要约束" : "存在约束边界压力";
        return "组合为 " + mix + "，预测灰分 " + ash + "%、硫分 " + sulfur + "%、发热量 "
                + calorific.setScale(0, RoundingMode.HALF_UP) + " kcal/kg；质量分 "
                + score.getQualityScore() + "、成本分 " + score.getCostScore() + "、稳定性分 "
                + score.getStabilityScore() + "，" + status + "。";
    }

    private String buildRiskTip(ConstraintResult constraint) {
        List<String> risks = new ArrayList<>();
        risks.addAll(constraint.getViolations());
        risks.addAll(constraint.getWarnings());
        return risks.isEmpty() ? null : String.join("；", risks);
    }

    private boolean passesUpperConstraint(BigDecimal target, BigDecimal actual) {
        return target == null || actual == null || actual.compareTo(target) <= 0;
    }

    private boolean passesLowerConstraint(BigDecimal target, BigDecimal actual) {
        return target == null || actual == null || actual.compareTo(target) >= 0;
    }

    private BigDecimal penaltyUpper(BigDecimal actual, BigDecimal target, BigDecimal maxPenalty) {
        if (actual == null || target == null || actual.compareTo(target) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal gapRatio = actual.subtract(target)
                .divide(target.max(ONE), 4, RoundingMode.HALF_UP);
        return maxPenalty.multiply(gapRatio.multiply(new BigDecimal("4")).min(ONE))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal penaltyLower(BigDecimal actual, BigDecimal target, BigDecimal maxPenalty) {
        if (actual == null || target == null || actual.compareTo(target) >= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal gapRatio = target.subtract(actual)
                .divide(target.max(ONE), 4, RoundingMode.HALF_UP);
        return maxPenalty.multiply(gapRatio.multiply(new BigDecimal("4")).min(ONE))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal rewardUpper(BigDecimal actual, BigDecimal target, BigDecimal maxReward) {
        if (actual == null || target == null || actual.compareTo(target) > 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal marginRatio = target.subtract(actual)
                .divide(target.max(ONE), 4, RoundingMode.HALF_UP);
        return maxReward.min(marginRatio.multiply(new BigDecimal("20"))).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal rewardLower(BigDecimal actual, BigDecimal target, BigDecimal maxReward) {
        if (actual == null || target == null || actual.compareTo(target) < 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal marginRatio = actual.subtract(target)
                .divide(target.max(ONE), 4, RoundingMode.HALF_UP);
        return maxReward.min(marginRatio.multiply(new BigDecimal("15"))).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal clampScore(BigDecimal score) {
        if (score.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (score.compareTo(HUNDRED) > 0) {
            return HUNDRED.setScale(2, RoundingMode.HALF_UP);
        }
        return score.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal nzMetric(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
