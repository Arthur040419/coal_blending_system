package com.coalblend.service.knowledge;

import com.coalblend.dto.knowledge.KnowledgeContextDTO;
import com.coalblend.entity.BlendPlan;
import com.coalblend.entity.CoalType;
import com.coalblend.entity.Inventory;
import com.coalblend.entity.Orders;
import com.coalblend.vo.blend.PlanDetailVO;
import com.coalblend.vo.blend.PlanWithDetailsVO;
import com.coalblend.vo.knowledge.KnowledgeSummaryVO;
import com.coalblend.vo.knowledge.MatchedCaseVO;
import com.coalblend.vo.knowledge.MatchedRuleVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class KnowledgeAssembleServiceImpl implements KnowledgeAssembleService {

    private static final BigDecimal STOCK_WARN = new BigDecimal("3000");
    private static final BigDecimal HIGH_STOCK = new BigDecimal("8000");

    @Override
    public KnowledgeContextDTO assemble(Orders order,
                                        Map<String, Object> constraints,
                                        Map<Long, CoalType> typeMap,
                                        Map<Long, Inventory> invMap,
                                        List<Long> rankedCoalIds,
                                        List<MatchedRuleVO> matchedRules,
                                        List<MatchedCaseVO> matchedCases,
                                        PlanWithDetailsVO recommendedPlan,
                                        BigDecimal orderDemandQuantity) {
        KnowledgeContextDTO dto = new KnowledgeContextDTO();
        dto.setMatchedRules(matchedRules == null ? List.of() : new ArrayList<>(matchedRules));
        dto.setMatchedCases(matchedCases == null ? List.of() : new ArrayList<>(matchedCases));

        copyOrderFields(order, dto);
        dto.setOrderSummary(buildOrderSummary(order));
        dto.setInventorySummary(buildInventorySummary(invMap, rankedCoalIds, typeMap));
        dto.setCoalSummary(buildCoalSummary(rankedCoalIds, typeMap));

        dto.setOrderText(buildOrderText(order, constraints));
        dto.setRulesText(buildRulesText(matchedRules));
        dto.setCasesText(buildCasesText(matchedCases));

        if (recommendedPlan != null && recommendedPlan.getPlan() != null) {
            List<PlanDetailVO> details = recommendedPlan.getDetails() == null ? List.of() : recommendedPlan.getDetails();
            Set<Long> planCoalIds = details.stream()
                    .map(PlanDetailVO::getCoalId)
                    .filter(id -> id != null)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            dto.setPlanDetailsText(buildPlanDetailsText(details, typeMap));
            dto.setPlanSummary(buildPlanSummary(recommendedPlan.getPlan()));
            dto.setPlanText(buildPlanText(recommendedPlan.getPlan(), dto.getPlanDetailsText()));
            dto.setInventoryText(buildInventoryTextForPlan(planCoalIds, invMap, typeMap, orderDemandQuantity));
        } else {
            dto.setPlanText("（暂无推荐方案）");
            dto.setPlanSummary("—");
            dto.setPlanDetailsText("");
            dto.setInventoryText(buildInventoryText(invMap, rankedCoalIds, typeMap, orderDemandQuantity));
        }
        return dto;
    }

    private static void copyOrderFields(Orders order, KnowledgeContextDTO dto) {
        if (order == null) {
            return;
        }
        dto.setOrderCode(order.getOrderCode());
        dto.setCustomerName(order.getCustomerName());
        dto.setDemandQuantity(order.getDemandQuantity());
        dto.setTargetAsh(order.getTargetAsh());
        dto.setTargetSulfur(order.getTargetSulfur());
        dto.setTargetMoisture(order.getTargetMoisture());
        dto.setTargetCalorific(order.getTargetCalorific());
        dto.setPriorityLevel(order.getPriorityLevel());
        dto.setDeliveryDate(order.getDeliveryDate());
    }

    @Override
    public KnowledgeSummaryVO summarize(KnowledgeContextDTO ctx) {
        KnowledgeSummaryVO s = new KnowledgeSummaryVO();
        if (ctx == null) {
            return s;
        }
        s.setOrderSummary(ctx.getOrderSummary());
        s.setInventorySummary(ctx.getInventorySummary());
        s.setPlanSummary(ctx.getPlanSummary());
        s.setRuleCount(ctx.getMatchedRules() == null ? 0 : ctx.getMatchedRules().size());
        s.setCaseCount(ctx.getMatchedCases() == null ? 0 : ctx.getMatchedCases().size());
        return s;
    }

    private static String buildOrderSummary(Orders order) {
        return String.format("订单 %s｜客户 %s｜需求量 %s 吨｜硫分≤%s｜热值≥%s",
                nz(order.getOrderCode()),
                nz(order.getCustomerName()),
                fmt(order.getDemandQuantity()),
                fmt(order.getTargetSulfur()),
                fmt(order.getTargetCalorific()));
    }

    private static String buildInventorySummary(Map<Long, Inventory> invMap, List<Long> rankedCoalIds,
                                                Map<Long, CoalType> typeMap) {
        if (invMap == null || rankedCoalIds == null) {
            return "（无库存摘要）";
        }
        long low = rankedCoalIds.stream()
                .map(invMap::get)
                .filter(inv -> inv != null && inv.getAvailableQuantity() != null)
                .filter(inv -> inv.getAvailableQuantity().compareTo(STOCK_WARN) < 0)
                .count();
        if (low > 0) {
            return "候选煤种中存在可用库存低于 3000 吨的条目，共 " + low + " 项相关。";
        }
        return "候选煤种库存整体可满足常规执行。";
    }

    private static String buildCoalSummary(List<Long> rankedCoalIds, Map<Long, CoalType> typeMap) {
        if (rankedCoalIds == null || rankedCoalIds.isEmpty()) {
            return "（无煤种摘要）";
        }
        return rankedCoalIds.stream()
                .map(cid -> {
                    CoalType t = typeMap == null ? null : typeMap.get(cid);
                    return t == null ? ("煤种#" + cid) : t.getCoalName();
                })
                .collect(Collectors.joining("、"));
    }

    private static String buildOrderText(Orders order, Map<String, Object> constraints) {
        StringBuilder sb = new StringBuilder();
        sb.append("订单编号：").append(nz(order.getOrderCode())).append("\n");
        sb.append("客户名称：").append(nz(order.getCustomerName())).append("\n");
        sb.append("需求量：").append(fmt(order.getDemandQuantity())).append(" 吨\n");
        sb.append("灰分要求：≤").append(fmt(order.getTargetAsh())).append("%\n");
        sb.append("硫分要求：≤").append(fmt(order.getTargetSulfur())).append("%\n");
        sb.append("水分要求：≤").append(fmt(order.getTargetMoisture())).append("%\n");
        sb.append("发热量要求：≥").append(fmt(order.getTargetCalorific())).append(" kcal/kg\n");
        sb.append("优先级：").append(order.getPriorityLevel() == null ? "—" : String.valueOf(order.getPriorityLevel()));
        if (order.getDeliveryDate() != null) {
            sb.append("\n交付日期：").append(order.getDeliveryDate());
        }
        if (constraints != null && !constraints.isEmpty()) {
            sb.append("\n（约束快照）").append(constraints);
        }
        return sb.toString();
    }

    private static String buildPlanSummary(BlendPlan plan) {
        return String.format("%s｜总成本 %s 元｜综合评分 %s",
                nz(plan.getPlanName()),
                plan.getTotalCost() == null ? "—" : plan.getTotalCost().toPlainString(),
                plan.getOverallScore() == null ? "—" : plan.getOverallScore().toPlainString());
    }

    private static String buildPlanText(BlendPlan plan, String detailsText) {
        StringBuilder sb = new StringBuilder();
        sb.append("方案名称：").append(nz(plan.getPlanName())).append("\n");
        sb.append("总成本：").append(plan.getTotalCost() == null ? "—" : plan.getTotalCost().toPlainString()).append(" 元\n");
        sb.append("质量评分：").append(fmt(plan.getQualityScore())).append("\n");
        sb.append("成本评分：").append(fmt(plan.getCostScore())).append("\n");
        sb.append("稳定性评分：").append(fmt(plan.getStabilityScore())).append("\n");
        sb.append("综合评分：").append(fmt(plan.getOverallScore())).append("\n");
        sb.append("方案状态：").append(nz(plan.getPlanStatus())).append("\n\n");
        sb.append("方案明细：\n");
        sb.append(StringUtils.hasText(detailsText) ? detailsText : "（无）");
        return sb.toString().trim();
    }

    private static String buildPlanDetailsText(List<PlanDetailVO> details, Map<Long, CoalType> typeMap) {
        if (details == null || details.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (PlanDetailVO d : details) {
            sb.append(formatDetailLine(d, typeMap)).append("\n");
        }
        return sb.toString().trim();
    }

    private static String formatDetailLine(PlanDetailVO d, Map<Long, CoalType> typeMap) {
        String name = d.getCoalName();
        if (!StringUtils.hasText(name) && d.getCoalId() != null && typeMap != null) {
            CoalType t = typeMap.get(d.getCoalId());
            name = t == null ? ("煤种ID" + d.getCoalId()) : t.getCoalName();
        }
        if (!StringUtils.hasText(name)) {
            name = d.getCoalId() == null ? "—" : ("煤种ID" + d.getCoalId());
        }
        BigDecimal pct = d.getBlendRatio() == null ? null
                : d.getBlendRatio().multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
        return String.format(
                "%s：配比%s%%，使用量%s吨，预测硫分%s%%，预测热值%s，单价%s元/吨",
                name,
                pct == null ? "—" : pct.toPlainString(),
                fmt(d.getUseQuantity()),
                fmt(d.getPredictedSulfur()),
                fmt(d.getPredictedCalorific()),
                fmt(d.getUnitCost())
        );
    }

    /** 仅候选煤种维度的库存摘录（无推荐方案时的回退） */
    private static String buildInventoryText(Map<Long, Inventory> invMap, List<Long> rankedCoalIds,
                                             Map<Long, CoalType> typeMap, BigDecimal demandQty) {
        if (invMap == null || rankedCoalIds == null) {
            return "（无库存信息）";
        }
        Set<Long> ids = new LinkedHashSet<>(rankedCoalIds);
        return buildInventoryTextForPlan(ids, invMap, typeMap, demandQty);
    }

    /**
     * 推荐方案涉及煤种的库存叙述 + 执行风险一句话（对齐方案 7.3 inventoryText 示例风格）。
     */
    private static String buildInventoryTextForPlan(Set<Long> planCoalIds, Map<Long, Inventory> invMap,
                                                     Map<Long, CoalType> typeMap, BigDecimal orderDemandQuantity) {
        if (invMap == null || planCoalIds == null || planCoalIds.isEmpty()) {
            return "（无与方案相关的库存信息）";
        }
        StringBuilder sb = new StringBuilder();
        boolean anyLow = false;
        boolean anyBelowDemand = false;
        int highCnt = 0;
        for (Long cid : planCoalIds) {
            Inventory inv = invMap.get(cid);
            CoalType t = typeMap == null ? null : typeMap.get(cid);
            String name = t == null ? ("煤种" + cid) : t.getCoalName();
            if (inv == null) {
                sb.append(name).append("：无库存记录\n");
                continue;
            }
            BigDecimal avail = inv.getAvailableQuantity();
            String qty = avail == null ? "—" : avail.toPlainString();
            sb.append(name).append("可用库存：").append(qty).append(" 吨");
            String tag;
            if (avail == null) {
                tag = "";
            } else if (avail.compareTo(STOCK_WARN) < 0) {
                tag = "，库存偏紧";
                anyLow = true;
            } else if (avail.compareTo(HIGH_STOCK) >= 0) {
                tag = "，库存充足";
                highCnt++;
            } else {
                tag = "，库存正常";
            }
            sb.append(tag);
            if (StringUtils.hasText(inv.getWarehouseCode())) {
                sb.append("（仓库").append(inv.getWarehouseCode()).append("）");
            }
            sb.append("\n");
            if (orderDemandQuantity != null && orderDemandQuantity.compareTo(BigDecimal.ZERO) > 0
                    && avail != null && avail.compareTo(orderDemandQuantity) < 0) {
                anyBelowDemand = true;
            }
        }
        if (anyBelowDemand) {
            sb.append("当前推荐方案涉及煤种中存在单库可用量低于本单总需求量的情况，执行前建议确认调拨或多仓合计。\n");
        } else if (anyLow) {
            sb.append("部分方案煤种库存偏低，请关注发运与现场执行节奏。\n");
        } else {
            sb.append("当前推荐方案涉及煤种库存整体可支撑本次执行，");
            sb.append(highCnt >= planCoalIds.size() ? "库存较为充足。\n" : "无明显库存不足风险。\n");
        }
        return sb.toString().trim();
    }

    private static String buildRulesText(List<MatchedRuleVO> rules) {
        if (rules == null || rules.isEmpty()) {
            return "（本场景未命中启用规则）";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rules.size(); i++) {
            MatchedRuleVO r = rules.get(i);
            sb.append("规则").append(i + 1).append("：").append(nz(r.getRuleName()))
                    .append("（").append(nz(r.getRuleType())).append("）\n");
            sb.append("   命中原因：").append(nz(r.getHitReason())).append("\n");
            if (StringUtils.hasText(r.getRuleContent())) {
                sb.append("   规则要点：").append(trim(r.getRuleContent(), 200)).append("\n");
            }
        }
        return sb.toString().trim();
    }

    private static String buildCasesText(List<MatchedCaseVO> cases) {
        if (cases == null || cases.isEmpty()) {
            return "（未检索到参考案例）";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cases.size(); i++) {
            MatchedCaseVO c = cases.get(i);
            sb.append("案例").append(i + 1).append("：").append(nz(c.getCaseName()))
                    .append("（").append(nz(c.getCaseCode())).append("）\n");
            sb.append("   摘要：").append(nz(c.getSummary())).append("\n");
            sb.append("   匹配原因：").append(nz(c.getMatchReason())).append("\n");
            if (StringUtils.hasText(c.getEffectivenessEval())) {
                sb.append("   效果评价：").append(c.getEffectivenessEval()).append("\n");
            }
        }
        return sb.toString().trim();
    }

    private static String nz(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }

    private static String fmt(BigDecimal v) {
        return v == null ? "—" : v.stripTrailingZeros().toPlainString();
    }

    private static String trim(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.replaceAll("\\s+", " ").trim();
        if (t.length() <= max) {
            return t;
        }
        return t.substring(0, max) + "…";
    }
}
