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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 配煤方案多维度评分引擎（v2 重构版）。
 *
 * <p>核心改进：</p>
 * <ol>
 *   <li>质量评分：从 "惩罚/奖励" 改为 sigmoid 标准化安全余量，消除天花板效应</li>
 *   <li>成本评分：使用全系统煤种价格区间做绝对基准，不再依赖单次候选池</li>
 *   <li>稳定性评分：库存阈值按订单需求量缩放，增加矿区多样性加分</li>
 *   <li>评分理由：每条理由包含具体数字，不再使用模板字符串</li>
 * </ol>
 */
@Service
public class PlanScoreServiceImpl implements PlanScoreService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    // ── sigmoid 斜率参数：控制评分对余量变化的敏感度 ──
    // 值越大，小余量就能拿到高分；值越小，需要大余量才能拉开差距
    private static final double ASH_K = 5.0;       // 灰分
    private static final double SULFUR_K = 4.0;    // 硫分（通常余量较小，斜率略低）
    private static final double MOISTURE_K = 4.0;  // 水分
    private static final double CALORIFIC_K = 3.0; // 发热量（热值余量通常较大，斜率最低）

    // ── 全系统煤种价格基准（从 CoalType.purchasePrice 统计得到，也可通过配置覆盖） ──
    // 当前种子数据中最低 360，最高 560
    private static final BigDecimal GLOBAL_MIN_PRICE = new BigDecimal("360");
    private static final BigDecimal GLOBAL_MAX_PRICE = new BigDecimal("560");

    @Override
    public EvaluatedPlanDraft evaluate(Orders order, List<PlanCoalSnapshot> snapshots, List<BigDecimal> ratios) {
        // ── 1. 加权预测各煤质指标 ──
        BigDecimal wAsh = ZERO, wS = ZERO, wM = ZERO, wV = ZERO, wCal = ZERO;
        BigDecimal totalCost = ZERO;

        ConstraintResult constraint = new ConstraintResult();
        List<BlendPlanDetail> details = new ArrayList<>();

        for (int i = 0; i < snapshots.size(); i++) {
            PlanCoalSnapshot snapshot = snapshots.get(i);
            BigDecimal r = ratios.get(i);
            CoalQuality q = snapshot.getQuality();
            CoalType t = snapshot.getType();
            Inventory inv = snapshot.getInventory();
            BigDecimal useQty = order.getDemandQuantity().multiply(r).setScale(2, RoundingMode.HALF_UP);
            BigDecimal price = t.getPurchasePrice() == null ? ZERO : t.getPurchasePrice();
            totalCost = totalCost.add(useQty.multiply(price));

            wAsh = wAsh.add(nzMetric(q.getAshContent()).multiply(r));
            wS = wS.add(nzMetric(q.getSulfurContent()).multiply(r));
            wM = wM.add(nzMetric(q.getMoistureContent()).multiply(r));
            wV = wV.add(nzMetric(q.getVolatileContent()).multiply(r));
            wCal = wCal.add(nzMetric(q.getCalorificValue()).multiply(r));

            // ── 库存约束校验 ──
            BigDecimal available = inv.getAvailableQuantity() == null ? ZERO : inv.getAvailableQuantity();
            BigDecimal remain = available.subtract(useQty);
            if (useQty.compareTo(available) > 0) {
                constraint.addViolation(t.getCoalName() + "用量超过可用库存");
            }
            // 库存紧张阈值按订单需求量缩放：需求量越大，安全线越高
            BigDecimal criticalThreshold = order.getDemandQuantity().multiply(new BigDecimal("0.15")).setScale(0, RoundingMode.HALF_UP);
            if (remain.compareTo(criticalThreshold) < 0 && useQty.compareTo(available) <= 0) {
                constraint.addWarning(t.getCoalName() + "执行后余量低于" + criticalThreshold + "吨");
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

        // ── 2. 多维度评分 ──
        ScoreDetail score = buildScoreDetail(order, details, snapshots, totalCost, pAsh, pS, pM, pCal);
        String explanation = buildPlanExplanation(snapshots, details, pAsh, pS, pCal, score, constraint.isFeasible());

        // ── 3. 组装结果 ──
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

    // ═══════════════════════════════════════════════
    //  指标预测与约束校验（与原版保持一致）
    // ═══════════════════════════════════════════════

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

    private void fillMetricConstraints(Orders order, ConstraintResult constraint, BigDecimal ash, BigDecimal sulfur,
                                       BigDecimal m, BigDecimal cal) {
        constraint.setPredictedAsh(ash);
        constraint.setPredictedSulfur(sulfur);
        constraint.setPredictedMoisture(m);
        constraint.setPredictedCalorific(cal);
        if (!passesUpperConstraint(order.getTargetSulfur(), sulfur)) {
            constraint.addViolation("预测硫分" + sulfur + "%超过订单上限" + order.getTargetSulfur() + "%");
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

    // ═══════════════════════════════════════════════
    //  多维度评分（v2 核心重构）
    // ═══════════════════════════════════════════════

    private ScoreDetail buildScoreDetail(Orders order, List<BlendPlanDetail> details,
                                         List<PlanCoalSnapshot> snapshots,
                                         BigDecimal totalCost, BigDecimal ash, BigDecimal sulfur,
                                         BigDecimal m, BigDecimal cal) {
        // ── 子维度评分 ──
        BigDecimal ashScore = scoreAshMargin(order, ash);
        BigDecimal sulfurScore = scoreSulfurMargin(order, sulfur);
        BigDecimal moistureScore = scoreMoistureMargin(order, m);
        BigDecimal calorificScore = scoreCalorificMargin(order, cal);

        BigDecimal qualityScore = ashScore.multiply(new BigDecimal("0.25"))
                .add(sulfurScore.multiply(new BigDecimal("0.35")))
                .add(moistureScore.multiply(new BigDecimal("0.15")))
                .add(calorificScore.multiply(new BigDecimal("0.25")))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal costScore = scoreCostAbsolute(totalCost, order.getDemandQuantity(), cal);

        // ── 矿区多样性 ──
        Set<String> mineAreas = snapshots.stream()
                .map(snap -> {
                    CoalType t = snap.getType();
                    return t.getCoalCategory() != null ? t.getCoalCategory() : t.getCoalName();
                })
                .collect(Collectors.toSet());
        int distinctAreas = mineAreas.size();

        BigDecimal stabilityScore = scoreStabilityScaled(details, snapshots, order.getDemandQuantity(), distinctAreas);

        // ── 综合评分 ──
        BigDecimal overall = qualityScore.multiply(new BigDecimal("0.50"))
                .add(costScore.multiply(new BigDecimal("0.20")))
                .add(stabilityScore.multiply(new BigDecimal("0.30")))
                .setScale(2, RoundingMode.HALF_UP);

        // ── 生成具体评分理由 ──
        ScoreDetail detail = new ScoreDetail();
        detail.setQualityScore(qualityScore);
        detail.setCostScore(costScore);
        detail.setStabilityScore(stabilityScore);
        detail.setOverallScore(overall);

        detail.setQualityReason(buildQualityReason(order, ash, sulfur, m, cal, ashScore, sulfurScore, moistureScore, calorificScore, qualityScore));
        detail.setCostReason(buildCostReason(totalCost, order.getDemandQuantity(), cal, costScore));
        detail.setStabilityReason(buildStabilityReason(details, snapshots, distinctAreas, stabilityScore));
        detail.setOverallReason(buildOverallReason(qualityScore, costScore, stabilityScore, overall));

        return detail;
    }

    // ═══════════════════════════════════════════════
    //  质量子维度：sigmoid 安全余量评分
    // ═══════════════════════════════════════════════

    /**
     * sigmoid(x) = 100 / (1 + exp(-k * x))
     * x = 标准化安全余量 = (target - actual) / target
     * 余量为负数（超标）时也适用，S 形曲线保证分数平滑下降
     */
    private static BigDecimal sigmoidScore(double margin, double k) {
        double raw = 100.0 / (1.0 + Math.exp(-k * margin));
        double clamped = Math.max(0.0, Math.min(100.0, raw));
        return BigDecimal.valueOf(clamped).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal scoreAshMargin(Orders order, BigDecimal actualAsh) {
        if (order.getTargetAsh() == null || actualAsh == null || order.getTargetAsh().compareTo(ZERO) <= 0) {
            return new BigDecimal("70.00");
        }
        double margin = order.getTargetAsh().subtract(actualAsh)
                .divide(order.getTargetAsh(), 8, RoundingMode.HALF_UP).doubleValue();
        return sigmoidScore(margin, ASH_K);
    }

    private BigDecimal scoreSulfurMargin(Orders order, BigDecimal actualSulfur) {
        if (order.getTargetSulfur() == null || actualSulfur == null || order.getTargetSulfur().compareTo(ZERO) <= 0) {
            return new BigDecimal("70.00");
        }
        double margin = order.getTargetSulfur().subtract(actualSulfur)
                .divide(order.getTargetSulfur(), 8, RoundingMode.HALF_UP).doubleValue();
        return sigmoidScore(margin, SULFUR_K);
    }

    private BigDecimal scoreMoistureMargin(Orders order, BigDecimal actualMoisture) {
        if (order.getTargetMoisture() == null || actualMoisture == null || order.getTargetMoisture().compareTo(ZERO) <= 0) {
            return new BigDecimal("70.00");
        }
        double margin = order.getTargetMoisture().subtract(actualMoisture)
                .divide(order.getTargetMoisture(), 8, RoundingMode.HALF_UP).doubleValue();
        return sigmoidScore(margin, MOISTURE_K);
    }

    private BigDecimal scoreCalorificMargin(Orders order, BigDecimal actualCalorific) {
        if (order.getTargetCalorific() == null || actualCalorific == null || order.getTargetCalorific().compareTo(ZERO) <= 0) {
            return new BigDecimal("70.00");
        }
        double margin = actualCalorific.subtract(order.getTargetCalorific())
                .divide(order.getTargetCalorific(), 8, RoundingMode.HALF_UP).doubleValue();
        return sigmoidScore(margin, CALORIFIC_K);
    }

    // ═══════════════════════════════════════════════
    //  成本评分：绝对价格基准 + 热值性价比
    // ═══════════════════════════════════════════════

    private BigDecimal scoreCostAbsolute(BigDecimal totalCost, BigDecimal demand, BigDecimal calorific) {
        if (demand == null || demand.compareTo(ZERO) <= 0) {
            return new BigDecimal("70.00");
        }
        BigDecimal unitCost = totalCost.divide(demand, 4, RoundingMode.HALF_UP);

        // 吨煤成本在全局价格区间中的位置
        BigDecimal priceRange = GLOBAL_MAX_PRICE.subtract(GLOBAL_MIN_PRICE);
        if (priceRange.compareTo(ZERO) <= 0) {
            return new BigDecimal("75.00");
        }
        BigDecimal normalized = GLOBAL_MAX_PRICE.subtract(unitCost)
                .divide(priceRange, 4, RoundingMode.HALF_UP);
        // 成本评分占 85%
        BigDecimal priceScore = new BigDecimal("40")
                .add(normalized.max(ZERO).min(ONE).multiply(new BigDecimal("45")));

        // 热值性价比占 15%：每元买到的大卡数
        if (calorific != null && calorific.compareTo(ZERO) > 0) {
            BigDecimal efficiency = calorific.divide(unitCost.max(ONE), 4, RoundingMode.HALF_UP);
            // 基准：360元买4500kcal → 12.5；560元买7200kcal → 12.86
            // 好的方案效率约 12-15，差的约 8-10
            double eff = efficiency.doubleValue();
            double effScore = Math.max(0, Math.min(100, 40 + (eff - 8.0) * 15.0));
            BigDecimal efficiencyScore = BigDecimal.valueOf(effScore);

            return priceScore.multiply(new BigDecimal("0.85"))
                    .add(efficiencyScore.multiply(new BigDecimal("0.15")))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        return priceScore.setScale(2, RoundingMode.HALF_UP);
    }

    // ═══════════════════════════════════════════════
    //  稳定性评分：阈值缩放 + 多样性加分
    // ═══════════════════════════════════════════════

    private BigDecimal scoreStabilityScaled(List<BlendPlanDetail> details, List<PlanCoalSnapshot> snapshots,
                                            BigDecimal demand, int distinctAreas) {
        if (details.isEmpty()) {
            return new BigDecimal("50.00");
        }
        // 阈值按需求量缩放：需求量越大，对库存覆盖倍数的要求越高
        BigDecimal demandScale = demand.divide(new BigDecimal("1000"), 4, RoundingMode.HALF_UP);
        double coverageTarget = 1.5 + demandScale.doubleValue() * 0.05; // 需求每增加1000吨，覆盖目标+0.05

        java.util.Map<Long, PlanCoalSnapshot> snapshotMap = snapshots.stream()
                .collect(Collectors.toMap(PlanCoalSnapshot::getCoalId, s -> s, (a, b) -> a));

        BigDecimal coverageSum = ZERO;
        int tightCount = 0;
        int validCount = 0;

        for (BlendPlanDetail d : details) {
            PlanCoalSnapshot snapshot = snapshotMap.get(d.getCoalId());
            if (snapshot == null || snapshot.getInventory().getAvailableQuantity() == null
                    || d.getUseQuantity() == null || d.getUseQuantity().compareTo(ZERO) <= 0) {
                continue;
            }
            BigDecimal available = snapshot.getInventory().getAvailableQuantity();
            BigDecimal coverage = available.divide(d.getUseQuantity(), 4, RoundingMode.HALF_UP);
            coverageSum = coverageSum.add(coverage);
            validCount++;

            // 余量是否紧张
            BigDecimal remain = available.subtract(d.getUseQuantity());
            BigDecimal warnThreshold = demand.multiply(new BigDecimal("0.12"));
            if (remain.compareTo(warnThreshold) < 0) {
                tightCount++;
            }
        }

        if (validCount == 0) {
            return new BigDecimal("50.00");
        }

        double avgCoverage = coverageSum.divide(new BigDecimal(validCount), 4, RoundingMode.HALF_UP).doubleValue();

        // 覆盖率评分：覆盖率=1时给55分，达到coverageTarget时约85分
        double coverageScore = 55 + Math.min(35, Math.max(0, (avgCoverage - 1.0) / (coverageTarget - 1.0) * 35));

        // 煤种数量加分
        double varietyBonus = 0;
        if (validCount >= 3) varietyBonus += 8;
        else if (validCount >= 2) varietyBonus += 4;

        // 矿区多样性加分
        double areaBonus = Math.min(6, (distinctAreas - 1) * 3);

        // 库存紧张扣分（按需求量阈值）
        double tightPenalty = tightCount * Math.min(15, 5 + demandScale.doubleValue());

        double raw = coverageScore + varietyBonus + areaBonus - tightPenalty;
        return clampScore(BigDecimal.valueOf(raw));
    }

    // ═══════════════════════════════════════════════
    //  具体评分理由生成
    // ═══════════════════════════════════════════════

    private String buildQualityReason(Orders order, BigDecimal ash, BigDecimal sulfur, BigDecimal m, BigDecimal cal,
                                      BigDecimal ashScore, BigDecimal sulfurScore, BigDecimal moistureScore,
                                      BigDecimal calorificScore, BigDecimal totalScore) {
        StringBuilder sb = new StringBuilder();
        sb.append("质量评分 ").append(totalScore).append("（灰分").append(ashScore)
                .append("×0.25 + 硫分").append(sulfurScore)
                .append("×0.35 + 水分").append(moistureScore)
                .append("×0.15 + 热值").append(calorificScore).append("×0.25）。");

        appendMarginLine(sb, "灰分", ash, order.getTargetAsh(), "%", false, ashScore);
        appendMarginLine(sb, "硫分", sulfur, order.getTargetSulfur(), "%", false, sulfurScore);
        appendMarginLine(sb, "水分", m, order.getTargetMoisture(), "%", false, moistureScore);
        appendMarginLine(sb, "发热量", cal, order.getTargetCalorific(), "kcal/kg", true, calorificScore);

        return sb.toString().trim();
    }

    private void appendMarginLine(StringBuilder sb, String label, BigDecimal actual, BigDecimal target,
                                  String unit, boolean higherIsBetter, BigDecimal score) {
        if (actual == null || target == null) return;
        BigDecimal diff = higherIsBetter ? actual.subtract(target) : target.subtract(actual);
        String direction = higherIsBetter ? "高于" : "低于";
        String status = diff.compareTo(ZERO) >= 0 ? "达标" : "超标";
        sb.append(label).append("预测").append(actual).append(unit).append("，")
                .append(direction).append("目标").append(target.abs()).append(unit)
                .append("，").append(status).append("（").append(score).append("分）；");
    }

    private String buildCostReason(BigDecimal totalCost, BigDecimal demand, BigDecimal cal, BigDecimal costScore) {
        if (demand == null || demand.compareTo(ZERO) <= 0) {
            return "成本评分 " + costScore + "。";
        }
        BigDecimal unitCost = totalCost.divide(demand, 2, RoundingMode.HALF_UP);
        StringBuilder sb = new StringBuilder();
        sb.append("成本评分 ").append(costScore).append("。")
                .append("吨煤成本").append(unitCost).append("元/吨，")
                .append("在全系统煤种价格区间（").append(GLOBAL_MIN_PRICE).append("-")
                .append(GLOBAL_MAX_PRICE).append("元/吨）中处于");

        BigDecimal midPrice = GLOBAL_MIN_PRICE.add(GLOBAL_MAX_PRICE).divide(new BigDecimal("2"), 0, RoundingMode.HALF_UP);
        if (unitCost.compareTo(midPrice) < 0) {
            sb.append("较低水平");
        } else if (unitCost.compareTo(midPrice) > 0) {
            sb.append("中高水平");
        } else {
            sb.append("中等水平");
        }

        if (cal != null && cal.compareTo(ZERO) > 0) {
            BigDecimal efficiency = cal.divide(unitCost.max(ONE), 1, RoundingMode.HALF_UP);
            sb.append("；热值性价比").append(efficiency).append("kcal/元");
        }
        sb.append("。");
        return sb.toString();
    }

    private String buildStabilityReason(List<BlendPlanDetail> details, List<PlanCoalSnapshot> snapshots,
                                         int distinctAreas, BigDecimal stabilityScore) {
        if (details.isEmpty()) {
            return "稳定性评分 " + stabilityScore + "。";
        }
        java.util.Map<Long, PlanCoalSnapshot> snapshotMap = snapshots.stream()
                .collect(Collectors.toMap(PlanCoalSnapshot::getCoalId, s -> s, (a, b) -> a));

        StringBuilder sb = new StringBuilder();
        sb.append("稳定性评分 ").append(stabilityScore).append("。");

        for (int i = 0; i < details.size(); i++) {
            BlendPlanDetail d = details.get(i);
            PlanCoalSnapshot snapshot = snapshotMap.get(d.getCoalId());
            if (snapshot == null || snapshot.getInventory().getAvailableQuantity() == null
                    || d.getUseQuantity() == null || d.getUseQuantity().compareTo(ZERO) <= 0) {
                continue;
            }
            BigDecimal available = snapshot.getInventory().getAvailableQuantity();
            BigDecimal coverage = available.divide(d.getUseQuantity(), 1, RoundingMode.HALF_UP);
            String name = snapshot.getType().getCoalName();
            sb.append(name).append("覆盖").append(coverage).append("倍；");
        }

        sb.append("共").append(details.size()).append("种煤，").append(distinctAreas).append("个来源矿区。");
        return sb.toString();
    }

    private String buildOverallReason(BigDecimal quality, BigDecimal cost, BigDecimal stability, BigDecimal overall) {
        return "综合评分 " + overall + " = 质量" + quality + "×50% + 成本" + cost
                + "×20% + 稳定性" + stability + "×30%。";
    }

    // ═══════════════════════════════════════════════
    //  方案解释与风险提示
    // ═══════════════════════════════════════════════

    private String buildPlanExplanation(List<PlanCoalSnapshot> snapshots, List<BlendPlanDetail> details,
                                        BigDecimal ash, BigDecimal sulfur, BigDecimal calorific,
                                        ScoreDetail score, boolean feasible) {
        String mix = details.stream()
                .map(d -> {
                    PlanCoalSnapshot snapshot = snapshots.stream()
                            .filter(s -> s.getCoalId().equals(d.getCoalId()))
                            .findFirst().orElse(null);
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

    // ═══════════════════════════════════════════════
    //  工具方法
    // ═══════════════════════════════════════════════

    private boolean passesUpperConstraint(BigDecimal target, BigDecimal actual) {
        return target == null || actual == null || actual.compareTo(target) <= 0;
    }

    private boolean passesLowerConstraint(BigDecimal target, BigDecimal actual) {
        return target == null || actual == null || actual.compareTo(target) >= 0;
    }

    private BigDecimal clampScore(BigDecimal score) {
        if (score.compareTo(ZERO) < 0) return ZERO.setScale(2, RoundingMode.HALF_UP);
        if (score.compareTo(HUNDRED) > 0) return HUNDRED.setScale(2, RoundingMode.HALF_UP);
        return score.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal nzMetric(BigDecimal value) {
        return value == null ? ZERO : value;
    }
}
