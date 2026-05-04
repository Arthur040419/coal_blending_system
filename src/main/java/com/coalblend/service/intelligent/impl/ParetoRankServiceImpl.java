package com.coalblend.service.intelligent.impl;

import com.coalblend.entity.BlendPlanDetail;
import com.coalblend.entity.Orders;
import com.coalblend.service.intelligent.ParetoRankService;
import com.coalblend.service.intelligent.model.EvaluatedPlanDraft;
import com.coalblend.service.intelligent.model.PlanCoalSnapshot;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ParetoRankServiceImpl implements ParetoRankService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    @Override
    public void rank(Orders order, List<EvaluatedPlanDraft> drafts) {
        if (drafts == null || drafts.isEmpty()) {
            return;
        }
        for (EvaluatedPlanDraft draft : drafts) {
            fillObjectives(order, draft);
        }
        Map<String, List<EvaluatedPlanDraft>> groups = drafts.stream()
                .collect(Collectors.groupingBy(EvaluatedPlanDraft::getDecisionStatus, LinkedHashMap::new, Collectors.toList()));
        for (List<EvaluatedPlanDraft> group : groups.values()) {
            rankGroup(group);
        }
    }

    private void fillObjectives(Orders order, EvaluatedPlanDraft draft) {
        BigDecimal demand = order == null ? null : order.getDemandQuantity();
        if (demand != null && demand.compareTo(ZERO) > 0 && draft.getTotalCost() != null) {
            draft.setObjectiveCostPerTon(draft.getTotalCost().divide(demand, 4, RoundingMode.HALF_UP));
        } else {
            draft.setObjectiveCostPerTon(ZERO.setScale(4, RoundingMode.HALF_UP));
        }
        draft.setObjectiveQualityDeviation(qualityDeviation(order, draft).setScale(4, RoundingMode.HALF_UP));
        draft.setObjectiveExecutionRisk(executionRisk(draft).setScale(4, RoundingMode.HALF_UP));
    }

    private BigDecimal qualityDeviation(Orders order, EvaluatedPlanDraft draft) {
        List<BlendPlanDetail> details = draft.getDetails() == null ? List.of() : draft.getDetails();
        BigDecimal ash = firstPredicted(details, BlendPlanDetail::getPredictedAsh);
        BigDecimal sulfur = firstPredicted(details, BlendPlanDetail::getPredictedSulfur);
        BigDecimal moisture = firstPredicted(details, BlendPlanDetail::getPredictedMoisture);
        BigDecimal volatileValue = firstPredicted(details, BlendPlanDetail::getPredictedVolatile);
        BigDecimal calorific = firstPredicted(details, BlendPlanDetail::getPredictedCalorific);
        return deviation(ash, order == null ? null : order.getTargetAsh()).multiply(new BigDecimal("0.25"))
                .add(deviation(sulfur, order == null ? null : order.getTargetSulfur()).multiply(new BigDecimal("0.30")))
                .add(deviation(moisture, order == null ? null : order.getTargetMoisture()).multiply(new BigDecimal("0.15")))
                .add(deviation(calorific, order == null ? null : order.getTargetCalorific()).multiply(new BigDecimal("0.25")))
                .add(deviation(volatileValue, order == null ? null : order.getTargetVolatile()).multiply(new BigDecimal("0.05")));
    }

    private BigDecimal executionRisk(EvaluatedPlanDraft draft) {
        List<BlendPlanDetail> details = draft.getDetails() == null ? List.of() : draft.getDetails();
        List<PlanCoalSnapshot> snapshots = draft.getSnapshots() == null ? List.of() : draft.getSnapshots();
        BigDecimal maxInventoryUsage = ZERO;
        BigDecimal maxRatio = ZERO;
        for (int i = 0; i < details.size(); i++) {
            BlendPlanDetail detail = details.get(i);
            if (detail.getBlendRatio() != null && detail.getBlendRatio().compareTo(maxRatio) > 0) {
                maxRatio = detail.getBlendRatio();
            }
            PlanCoalSnapshot snapshot = i < snapshots.size() ? snapshots.get(i) : null;
            if (snapshot == null || snapshot.getInventory() == null || snapshot.getInventory().getAvailableQuantity() == null
                    || snapshot.getInventory().getAvailableQuantity().compareTo(ZERO) <= 0 || detail.getUseQuantity() == null) {
                continue;
            }
            BigDecimal usage = detail.getUseQuantity().divide(snapshot.getInventory().getAvailableQuantity(), 6, RoundingMode.HALF_UP);
            if (usage.compareTo(maxInventoryUsage) > 0) {
                maxInventoryUsage = usage;
            }
        }
        return maxInventoryUsage.multiply(new BigDecimal("0.60"))
                .add(maxRatio.multiply(new BigDecimal("0.40")));
    }

    private void rankGroup(List<EvaluatedPlanDraft> group) {
        if (group == null || group.isEmpty()) {
            return;
        }
        Map<EvaluatedPlanDraft, Set<EvaluatedPlanDraft>> dominates = new HashMap<>();
        Map<EvaluatedPlanDraft, Integer> dominatedCount = new HashMap<>();
        for (EvaluatedPlanDraft a : group) {
            dominates.put(a, new HashSet<>());
            dominatedCount.put(a, 0);
        }
        for (int i = 0; i < group.size(); i++) {
            EvaluatedPlanDraft a = group.get(i);
            for (int j = 0; j < group.size(); j++) {
                if (i == j) {
                    continue;
                }
                EvaluatedPlanDraft b = group.get(j);
                if (dominates(a, b)) {
                    dominates.get(a).add(b);
                } else if (dominates(b, a)) {
                    dominatedCount.put(a, dominatedCount.get(a) + 1);
                }
            }
        }
        for (EvaluatedPlanDraft draft : group) {
            draft.setDominatedCount(dominatedCount.getOrDefault(draft, 0));
            draft.setDominatesCount(dominates.getOrDefault(draft, Set.of()).size());
        }

        List<EvaluatedPlanDraft> remaining = new ArrayList<>(group);
        int rank = 1;
        while (!remaining.isEmpty()) {
            List<EvaluatedPlanDraft> front = remaining.stream()
                    .filter(candidate -> remaining.stream()
                            .noneMatch(other -> other != candidate && dominates(other, candidate)))
                    .sorted(Comparator.comparing(EvaluatedPlanDraft::getObjectiveCostPerTon))
                    .toList();
            if (front.isEmpty()) {
                for (EvaluatedPlanDraft candidate : remaining) {
                    candidate.setParetoRank(rank);
                }
                break;
            }
            for (EvaluatedPlanDraft candidate : front) {
                candidate.setParetoRank(rank);
            }
            remaining.removeAll(front);
            rank++;
        }
    }

    private boolean dominates(EvaluatedPlanDraft a, EvaluatedPlanDraft b) {
        int cost = cmp(a.getObjectiveCostPerTon(), b.getObjectiveCostPerTon());
        int quality = cmp(a.getObjectiveQualityDeviation(), b.getObjectiveQualityDeviation());
        int risk = cmp(a.getObjectiveExecutionRisk(), b.getObjectiveExecutionRisk());
        return cost <= 0 && quality <= 0 && risk <= 0 && (cost < 0 || quality < 0 || risk < 0);
    }

    private int cmp(BigDecimal a, BigDecimal b) {
        BigDecimal av = a == null ? ZERO : a;
        BigDecimal bv = b == null ? ZERO : b;
        return av.compareTo(bv);
    }

    private BigDecimal deviation(BigDecimal actual, BigDecimal target) {
        if (actual == null || target == null || target.compareTo(ZERO) <= 0) {
            return ZERO;
        }
        return actual.subtract(target).abs().divide(target, 6, RoundingMode.HALF_UP);
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
}
