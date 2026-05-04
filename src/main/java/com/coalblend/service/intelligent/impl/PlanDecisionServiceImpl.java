package com.coalblend.service.intelligent.impl;

import com.coalblend.entity.BlendPlanDetail;
import com.coalblend.entity.CoalQuality;
import com.coalblend.entity.CoalType;
import com.coalblend.entity.Inventory;
import com.coalblend.entity.Orders;
import com.coalblend.enums.DecisionProblemSeverity;
import com.coalblend.enums.DecisionProblemType;
import com.coalblend.enums.PlanDecisionStatus;
import com.coalblend.service.intelligent.PlanDecisionService;
import com.coalblend.service.intelligent.model.BlendGenerationRuntimeConfig;
import com.coalblend.service.intelligent.model.ConstraintResult;
import com.coalblend.service.intelligent.model.EvaluatedPlanDraft;
import com.coalblend.service.intelligent.model.PlanCoalSnapshot;
import com.coalblend.vo.blend.DecisionProblemItemVO;
import com.coalblend.vo.blend.DecisionSuggestionItemVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class PlanDecisionServiceImpl implements PlanDecisionService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final BigDecimal RATIO_TOLERANCE = new BigDecimal("0.0001");

    @Override
    public void decide(Orders order, List<EvaluatedPlanDraft> drafts, BlendGenerationRuntimeConfig runtimeConfig) {
        if (drafts == null || drafts.isEmpty()) {
            return;
        }
        CostRange costRange = buildCostRange(order, drafts);
        for (EvaluatedPlanDraft draft : drafts) {
            List<DecisionProblemItemVO> problems = new ArrayList<>();
            collectHardProblems(order, draft, runtimeConfig, problems);
            collectWarnings(order, draft, runtimeConfig, costRange, problems);

            boolean hasBlocker = problems.stream()
                    .anyMatch(p -> DecisionProblemSeverity.BLOCKER.name().equals(p.getSeverity()));
            boolean hasWarning = problems.stream()
                    .anyMatch(p -> DecisionProblemSeverity.WARNING.name().equals(p.getSeverity()));
            PlanDecisionStatus status = hasBlocker ? PlanDecisionStatus.INFEASIBLE
                    : (hasWarning ? PlanDecisionStatus.RISKY : PlanDecisionStatus.FEASIBLE);
            draft.setDecisionStatus(status.name());
            draft.setDecisionStatusLabel(status.getLabel());
            draft.setProblemItems(problems);
            draft.setSuggestionItems(buildSuggestions(problems));
            if (!problems.isEmpty()) {
                draft.setRiskTip(problems.stream()
                        .map(DecisionProblemItemVO::getMessage)
                        .filter(StringUtils::hasText)
                        .distinct()
                        .reduce((a, b) -> a + "；" + b)
                        .orElse(draft.getRiskTip()));
            }
            syncConstraintSummary(draft, problems);
        }
    }

    private void collectHardProblems(Orders order, EvaluatedPlanDraft draft, BlendGenerationRuntimeConfig config,
                                     List<DecisionProblemItemVO> problems) {
        List<BigDecimal> ratios = draft.getRatios() == null ? List.of() : draft.getRatios();
        BigDecimal sum = ratios.stream().filter(Objects::nonNull).reduce(ZERO, BigDecimal::add);
        if (sum.subtract(ONE).abs().compareTo(RATIO_TOLERANCE) > 0) {
            problems.add(problem(DecisionProblemType.RATIO_SUM_INVALID, DecisionProblemSeverity.BLOCKER,
                    "blendRatio", "配比总和为 " + fmt(sum) + "，未等于 1.0000。",
                    sum, ONE, sum.subtract(ONE).abs(), null, null, null));
        }

        int materialCount = draft.getDetails() == null ? 0 : draft.getDetails().size();
        int maxMaterialCount = config == null || config.getMaxMaterialCount() == null ? 3 : config.getMaxMaterialCount();
        if (materialCount < 2 || materialCount > maxMaterialCount) {
            problems.add(problem(DecisionProblemType.MATERIAL_COUNT_INVALID, DecisionProblemSeverity.BLOCKER,
                    "materialCount", "配煤种类数量为 " + materialCount + "，要求不少于 2 且不超过 " + maxMaterialCount + "。",
                    BigDecimal.valueOf(materialCount), BigDecimal.valueOf(maxMaterialCount),
                    BigDecimal.valueOf(Math.abs(materialCount - maxMaterialCount)), null, null, null));
        }

        List<BlendPlanDetail> details = draft.getDetails() == null ? List.of() : draft.getDetails();
        List<PlanCoalSnapshot> snapshots = draft.getSnapshots() == null ? List.of() : draft.getSnapshots();
        for (int i = 0; i < details.size(); i++) {
            BlendPlanDetail detail = details.get(i);
            PlanCoalSnapshot snapshot = i < snapshots.size() ? snapshots.get(i) : null;
            CoalType type = snapshot == null ? null : snapshot.getType();
            CoalQuality quality = snapshot == null ? null : snapshot.getQuality();
            Inventory inventory = snapshot == null ? null : snapshot.getInventory();
            String coalName = type == null ? ("煤种" + detail.getCoalId()) : type.getCoalName();

            if (quality == null || quality.getAshContent() == null || quality.getSulfurContent() == null
                    || quality.getMoistureContent() == null || quality.getCalorificValue() == null) {
                problems.add(problem(DecisionProblemType.QUALITY_MISSING, DecisionProblemSeverity.BLOCKER,
                        "coalQuality", coalName + " 煤质数据不完整，无法作为可执行方案。",
                        null, null, null, null, detail.getCoalId(), coalName));
            }
            if (type == null || type.getPurchasePrice() == null) {
                problems.add(problem(DecisionProblemType.PRICE_MISSING, DecisionProblemSeverity.BLOCKER,
                        "purchasePrice", coalName + " 煤种价格缺失，无法完成成本核算。",
                        null, null, null, "元/吨", detail.getCoalId(), coalName));
            }
            BigDecimal useQty = nz(detail.getUseQuantity());
            BigDecimal available = inventory == null ? ZERO : nz(inventory.getAvailableQuantity());
            if (inventory != null && useQty.compareTo(available) > 0) {
                BigDecimal gap = useQty.subtract(available);
                problems.add(problem(DecisionProblemType.INVENTORY_NOT_ENOUGH, DecisionProblemSeverity.BLOCKER,
                        "availableQuantity", coalName + " 计划使用 " + fmt(useQty) + " 吨，可用库存 "
                                + fmt(available) + " 吨，缺口 " + fmt(gap) + " 吨。",
                        useQty, available, gap, "吨", detail.getCoalId(), coalName));
            }
        }

        BigDecimal ash = firstPredicted(details, BlendPlanDetail::getPredictedAsh);
        BigDecimal sulfur = firstPredicted(details, BlendPlanDetail::getPredictedSulfur);
        BigDecimal moisture = firstPredicted(details, BlendPlanDetail::getPredictedMoisture);
        BigDecimal calorific = firstPredicted(details, BlendPlanDetail::getPredictedCalorific);
        addUpperBlocker(problems, DecisionProblemType.ASH_EXCEED, "predictedAsh", "预测灰分",
                ash, order.getTargetAsh(), "%");
        addUpperBlocker(problems, DecisionProblemType.SULFUR_EXCEED, "predictedSulfur", "预测硫分",
                sulfur, order.getTargetSulfur(), "%");
        addUpperBlocker(problems, DecisionProblemType.MOISTURE_EXCEED, "predictedMoisture", "预测水分",
                moisture, order.getTargetMoisture(), "%");
        if (order.getTargetCalorific() != null && calorific != null
                && calorific.compareTo(order.getTargetCalorific()) < 0) {
            BigDecimal deviation = order.getTargetCalorific().subtract(calorific);
            problems.add(problem(DecisionProblemType.CALORIFIC_NOT_ENOUGH, DecisionProblemSeverity.BLOCKER,
                    "predictedCalorific", "预测发热量 " + fmt(calorific) + " kcal/kg 低于订单下限 "
                            + fmt(order.getTargetCalorific()) + " kcal/kg，缺口 " + fmt(deviation) + " kcal/kg。",
                    calorific, order.getTargetCalorific(), deviation, "kcal/kg", null, null));
        }
    }

    private void collectWarnings(Orders order, EvaluatedPlanDraft draft, BlendGenerationRuntimeConfig config,
                                 CostRange costRange, List<DecisionProblemItemVO> problems) {
        List<BlendPlanDetail> details = draft.getDetails() == null ? List.of() : draft.getDetails();
        List<PlanCoalSnapshot> snapshots = draft.getSnapshots() == null ? List.of() : draft.getSnapshots();
        BigDecimal lowQualityRate = config == null || config.getLowQualityMarginRate() == null
                ? new BigDecimal("0.05") : config.getLowQualityMarginRate();
        BigDecimal lowCalorificRate = config == null || config.getLowCalorificMarginRate() == null
                ? new BigDecimal("0.02") : config.getLowCalorificMarginRate();
        BigDecimal lowInventoryRate = config == null || config.getLowInventoryMarginRate() == null
                ? new BigDecimal("0.10") : config.getLowInventoryMarginRate();
        BigDecimal highSingleRatio = config == null || config.getHighSingleRatioThreshold() == null
                ? new BigDecimal("0.75") : config.getHighSingleRatioThreshold();

        addLowMarginWarning(problems, DecisionProblemType.ASH_EXCEED, "predictedAsh", "灰分安全余量",
                order.getTargetAsh(), firstPredicted(details, BlendPlanDetail::getPredictedAsh), lowQualityRate, "%", false);
        addLowMarginWarning(problems, DecisionProblemType.SULFUR_EXCEED, "predictedSulfur", "硫分安全余量",
                order.getTargetSulfur(), firstPredicted(details, BlendPlanDetail::getPredictedSulfur), lowQualityRate, "%", false);
        addLowMarginWarning(problems, DecisionProblemType.MOISTURE_EXCEED, "predictedMoisture", "水分安全余量",
                order.getTargetMoisture(), firstPredicted(details, BlendPlanDetail::getPredictedMoisture), lowQualityRate, "%", false);
        addLowMarginWarning(problems, DecisionProblemType.CALORIFIC_NOT_ENOUGH, "predictedCalorific", "发热量安全余量",
                order.getTargetCalorific(), firstPredicted(details, BlendPlanDetail::getPredictedCalorific), lowCalorificRate,
                "kcal/kg", true);

        BigDecimal volatileValue = firstPredicted(details, BlendPlanDetail::getPredictedVolatile);
        if (order.getTargetVolatile() != null && positive(order.getTargetVolatile()) && volatileValue != null) {
            BigDecimal deviation = volatileValue.subtract(order.getTargetVolatile()).abs();
            if (deviation.compareTo(new BigDecimal("5")) > 0) {
                problems.add(problem(DecisionProblemType.VOLATILE_DEVIATION, DecisionProblemSeverity.WARNING,
                        "predictedVolatile", "预测挥发分 " + fmt(volatileValue) + "% 与订单参考值 "
                                + fmt(order.getTargetVolatile()) + "% 偏差 " + fmt(deviation) + "%，超过 5%。",
                        volatileValue, order.getTargetVolatile(), deviation, "%", null, null));
            }
        }

        for (int i = 0; i < details.size(); i++) {
            BlendPlanDetail detail = details.get(i);
            PlanCoalSnapshot snapshot = i < snapshots.size() ? snapshots.get(i) : null;
            CoalType type = snapshot == null ? null : snapshot.getType();
            Inventory inventory = snapshot == null ? null : snapshot.getInventory();
            String coalName = type == null ? ("煤种" + detail.getCoalId()) : type.getCoalName();
            BigDecimal ratio = detail.getBlendRatio();
            if (ratio != null && ratio.compareTo(highSingleRatio) > 0) {
                BigDecimal pct = ratio.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP);
                problems.add(problem(DecisionProblemType.RATIO_SINGLE_TOO_HIGH, DecisionProblemSeverity.WARNING,
                        "blendRatio", coalName + " 单煤配比 " + pct + "% 高于阈值 "
                                + highSingleRatio.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP) + "%。",
                        ratio, highSingleRatio, ratio.subtract(highSingleRatio), null, detail.getCoalId(), coalName));
            }
            if (inventory == null || inventory.getAvailableQuantity() == null || inventory.getAvailableQuantity().compareTo(ZERO) <= 0) {
                continue;
            }
            BigDecimal available = inventory.getAvailableQuantity();
            BigDecimal remain = available.subtract(nz(detail.getUseQuantity()));
            BigDecimal marginRate = remain.divide(available, 6, RoundingMode.HALF_UP);
            if (marginRate.compareTo(lowInventoryRate) < 0 && remain.compareTo(ZERO) >= 0) {
                problems.add(problem(DecisionProblemType.INVENTORY_MARGIN_LOW, DecisionProblemSeverity.WARNING,
                        "availableQuantity", coalName + " 执行后库存余量率 " + percent(marginRate)
                                + " 低于安全阈值 " + percent(lowInventoryRate) + "。",
                        marginRate, lowInventoryRate, lowInventoryRate.subtract(marginRate).abs(), null,
                        detail.getCoalId(), coalName));
            }
        }

        if (costRange.rangePositive()) {
            BigDecimal costPerTon = costPerTon(order, draft);
            BigDecimal normalized = costPerTon.subtract(costRange.min())
                    .divide(costRange.max().subtract(costRange.min()), 6, RoundingMode.HALF_UP);
            if (normalized.compareTo(new BigDecimal("0.80")) >= 0) {
                problems.add(problem(DecisionProblemType.COST_TOO_HIGH, DecisionProblemSeverity.WARNING,
                        "objectiveCostPerTon", "当前候选吨煤成本 " + fmt(costPerTon)
                                + " 元/吨，处于候选池成本最高 20% 区间。",
                        costPerTon, costRange.max(), normalized, "元/吨", null, null));
            }
        }
    }

    private void addUpperBlocker(List<DecisionProblemItemVO> problems, DecisionProblemType type, String fieldName,
                                 String label, BigDecimal actual, BigDecimal target, String unit) {
        if (target == null || actual == null || actual.compareTo(target) <= 0) {
            return;
        }
        BigDecimal deviation = actual.subtract(target);
        problems.add(problem(type, DecisionProblemSeverity.BLOCKER, fieldName,
                label + " " + fmt(actual) + unit + " 超过订单上限 " + fmt(target) + unit
                        + "，超出 " + fmt(deviation) + unit + "。",
                actual, target, deviation, unit, null, null));
    }

    private void addLowMarginWarning(List<DecisionProblemItemVO> problems, DecisionProblemType type, String fieldName,
                                     String label, BigDecimal target, BigDecimal actual, BigDecimal rate,
                                     String unit, boolean higherIsBetter) {
        if (target == null || actual == null || !positive(target)) {
            return;
        }
        BigDecimal margin = higherIsBetter ? actual.subtract(target) : target.subtract(actual);
        if (margin.compareTo(ZERO) < 0) {
            return;
        }
        BigDecimal threshold = target.multiply(rate);
        if (margin.compareTo(threshold) < 0) {
            problems.add(problem(type, DecisionProblemSeverity.WARNING, fieldName,
                    label + " " + fmt(margin) + unit + " 低于预警阈值 " + fmt(threshold) + unit + "。",
                    actual, target, threshold.subtract(margin), unit, null, null));
        }
    }

    private void syncConstraintSummary(EvaluatedPlanDraft draft, List<DecisionProblemItemVO> problems) {
        ConstraintResult constraint = draft.getConstraintResult();
        if (constraint == null || problems == null || problems.isEmpty()) {
            return;
        }
        for (DecisionProblemItemVO item : problems) {
            if (!StringUtils.hasText(item.getMessage())) {
                continue;
            }
            if (DecisionProblemSeverity.BLOCKER.name().equals(item.getSeverity())
                    && !constraint.getViolations().contains(item.getMessage())) {
                constraint.addViolation(item.getMessage());
            } else if (DecisionProblemSeverity.WARNING.name().equals(item.getSeverity())
                    && !constraint.getWarnings().contains(item.getMessage())) {
                constraint.addWarning(item.getMessage());
            }
        }
    }

    private List<DecisionSuggestionItemVO> buildSuggestions(List<DecisionProblemItemVO> problems) {
        Map<String, DecisionSuggestionItemVO> map = new LinkedHashMap<>();
        for (DecisionProblemItemVO problem : problems) {
            SuggestionTemplate template = suggestionTemplate(problem.getType());
            if (template == null || map.containsKey(problem.getType())) {
                continue;
            }
            DecisionSuggestionItemVO item = new DecisionSuggestionItemVO();
            item.setType(problem.getType());
            item.setAction(template.action());
            item.setMessage(template.message());
            item.setPriority(DecisionProblemSeverity.BLOCKER.name().equals(problem.getSeverity()) ? 1 : 2);
            map.put(problem.getType(), item);
        }
        return map.values().stream()
                .sorted(Comparator.comparing(DecisionSuggestionItemVO::getPriority)
                        .thenComparing(DecisionSuggestionItemVO::getType))
                .toList();
    }

    private SuggestionTemplate suggestionTemplate(String type) {
        if (DecisionProblemType.SULFUR_EXCEED.name().equals(type)) {
            return new SuggestionTemplate("REDUCE_HIGH_SULFUR", "降低高硫煤占比，增加低硫煤候选或放宽硫分上限。");
        }
        if (DecisionProblemType.ASH_EXCEED.name().equals(type)) {
            return new SuggestionTemplate("ADD_LOW_ASH", "增加低灰煤比例，降低高灰煤参与比例。");
        }
        if (DecisionProblemType.MOISTURE_EXCEED.name().equals(type)) {
            return new SuggestionTemplate("ADD_LOW_MOISTURE", "增加低水分煤种比例，优先使用近期低水分煤质批次。");
        }
        if (DecisionProblemType.CALORIFIC_NOT_ENOUGH.name().equals(type)) {
            return new SuggestionTemplate("ADD_HIGH_CALORIFIC", "增加高热值煤比例，或放宽发热量下限。");
        }
        if (DecisionProblemType.INVENTORY_NOT_ENOUGH.name().equals(type)) {
            return new SuggestionTemplate("REPLENISH_OR_REDUCE", "补充对应煤种库存，或降低该煤种用量并重新生成方案。");
        }
        if (DecisionProblemType.INVENTORY_MARGIN_LOW.name().equals(type)) {
            return new SuggestionTemplate("IMPROVE_STOCK_MARGIN", "降低库存紧张煤种占比，提升库存余量安全边界。");
        }
        if (DecisionProblemType.RATIO_SINGLE_TOO_HIGH.name().equals(type)) {
            return new SuggestionTemplate("IMPROVE_RATIO_BALANCE", "引入第三种煤或降低主煤占比，提高方案稳定性。");
        }
        if (DecisionProblemType.COST_TOO_HIGH.name().equals(type)) {
            return new SuggestionTemplate("SWITCH_COST_FIRST", "切换为成本优先策略，或增加低价煤候选范围。");
        }
        return null;
    }

    private DecisionProblemItemVO problem(DecisionProblemType type, DecisionProblemSeverity severity,
                                          String fieldName, String message, BigDecimal actualValue,
                                          BigDecimal targetValue, BigDecimal deviation, String unit,
                                          Long coalId, String coalName) {
        DecisionProblemItemVO item = new DecisionProblemItemVO();
        item.setType(type.name());
        item.setTypeLabel(type.getLabel());
        item.setSeverity(severity.name());
        item.setSeverityLabel(severity.getLabel());
        item.setFieldName(fieldName);
        item.setMessage(message);
        item.setActualValue(scale(actualValue));
        item.setTargetValue(scale(targetValue));
        item.setDeviation(scale(deviation));
        item.setUnit(unit);
        item.setCoalId(coalId);
        item.setCoalName(coalName);
        return item;
    }

    private CostRange buildCostRange(Orders order, List<EvaluatedPlanDraft> drafts) {
        BigDecimal min = null;
        BigDecimal max = null;
        for (EvaluatedPlanDraft draft : drafts) {
            BigDecimal value = costPerTon(order, draft);
            if (value == null) {
                continue;
            }
            min = min == null || value.compareTo(min) < 0 ? value : min;
            max = max == null || value.compareTo(max) > 0 ? value : max;
        }
        return new CostRange(min, max);
    }

    private BigDecimal costPerTon(Orders order, EvaluatedPlanDraft draft) {
        if (order == null || order.getDemandQuantity() == null || order.getDemandQuantity().compareTo(ZERO) <= 0
                || draft == null || draft.getTotalCost() == null) {
            return null;
        }
        return draft.getTotalCost().divide(order.getDemandQuantity(), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal firstPredicted(List<BlendPlanDetail> details,
                                      java.util.function.Function<BlendPlanDetail, BigDecimal> getter) {
        if (details == null) {
            return null;
        }
        for (BlendPlanDetail detail : details) {
            BigDecimal value = getter.apply(detail);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private boolean positive(BigDecimal value) {
        return value != null && value.compareTo(ZERO) > 0;
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private BigDecimal scale(BigDecimal value) {
        return value == null ? null : value.setScale(4, RoundingMode.HALF_UP);
    }

    private String fmt(BigDecimal value) {
        return value == null ? "—" : value.stripTrailingZeros().toPlainString();
    }

    private String percent(BigDecimal value) {
        if (value == null) {
            return "—";
        }
        return value.multiply(new BigDecimal("100")).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "%";
    }

    private record SuggestionTemplate(String action, String message) {
    }

    private record CostRange(BigDecimal min, BigDecimal max) {
        boolean rangePositive() {
            return min != null && max != null && max.compareTo(min) > 0;
        }
    }
}
