package com.coalblend.service.intelligent.impl;

import com.coalblend.dto.AiExplainRequestDTO;
import com.coalblend.dto.knowledge.KnowledgeContextDTO;
import com.coalblend.entity.Orders;
import com.coalblend.service.intelligent.PromptBuildService;
import com.coalblend.vo.blend.PlanDetailVO;
import com.coalblend.vo.blend.PlanWithDetailsVO;
import com.coalblend.vo.knowledge.MatchedCaseVO;
import com.coalblend.vo.knowledge.MatchedRuleVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PromptBuildServiceImpl implements PromptBuildService {

    @Override
    public AiExplainRequestDTO buildRequest(Orders order, PlanWithDetailsVO recommended,
                                            List<MatchedRuleVO> matchedRules,
                                            List<MatchedCaseVO> matchedCases,
                                            KnowledgeContextDTO knowledgeContext) {
        AiExplainRequestDTO dto = new AiExplainRequestDTO();
        dto.setOrderCode(order.getOrderCode());
        dto.setCustomerName(order.getCustomerName());
        dto.setDemandQuantity(order.getDemandQuantity());
        dto.setTargetAsh(order.getTargetAsh());
        dto.setTargetSulfur(order.getTargetSulfur());
        dto.setTargetMoisture(order.getTargetMoisture());
        dto.setTargetCalorific(order.getTargetCalorific());
        dto.setPriorityLevel(order.getPriorityLevel());
        if (recommended != null && recommended.getPlan() != null) {
            var p = recommended.getPlan();
            dto.setPlanName(p.getPlanName());
            dto.setTotalCost(p.getTotalCost());
            dto.setQualityScore(p.getQualityScore());
            dto.setCostScore(p.getCostScore());
            dto.setStabilityScore(p.getStabilityScore());
            dto.setOverallScore(p.getOverallScore());
        }
        dto.setPlanDetails(recommended == null ? List.of() : recommended.getDetails());
        dto.setMatchedRules(matchedRules == null ? List.of() : matchedRules);
        dto.setMatchedCases(matchedCases == null ? List.of() : matchedCases);
        if (knowledgeContext != null) {
            dto.setKnowledgeOrderSummary(blankToEmpty(knowledgeContext.getOrderText()));
            dto.setKnowledgeInventorySummary(blankToEmpty(knowledgeContext.getInventoryText()));
        }
        return dto;
    }

    @Override
    public String buildPrompt(AiExplainRequestDTO dto, KnowledgeContextDTO ctx) {
        if (ctx != null && StringUtils.hasText(ctx.getPlanText())) {
            return buildKnowledgeEnhancedPrompt(ctx);
        }
        return buildLegacyPrompt(dto);
    }

    /**
     * 第2步：知识增强 Prompt（方案文档 7.2），输出四段式便于解析落库。
     */
    private String buildKnowledgeEnhancedPrompt(KnowledgeContextDTO ctx) {
        String orderT = nzBlock(ctx.getOrderText());
        String planT = nzBlock(ctx.getPlanText());
        String rulesT = nzBlock(formatRules(ctx.getMatchedRules()));
        String casesT = nzBlock(formatCases(ctx.getMatchedCases()));
        String invT = nzBlock(ctx.getInventoryText());

        return """
                你是煤矿智能配煤领域专家，请根据以下订单信息、推荐方案、命中规则、参考案例和库存信息，生成专业解释。

                【订单信息】
                %s

                【推荐方案】
                %s

                【命中规则】
                %s

                【参考案例】
                %s

                【库存信息】
                %s

                请按如下格式输出（必须保留序号与小标题，便于系统解析）：
                1. 方案说明：
                2. 规则依据：
                3. 风险提示：
                4. 优化建议：

                要求：
                - 使用正式、简洁、专业的中文；可使用 Markdown（如 **加粗**、列表），不要使用 HTML 标签；
                - 内容必须围绕当前订单与推荐方案，结合命中规则与参考案例；
                - 「规则依据」须明确写出与命中规则的对应关系，不要空泛；
                - 「风险提示」须结合库存、成本与质量约束；
                - 「优化建议」须可执行、可落地。
                """.formatted(orderT, planT, rulesT, casesT, invT);
    }

    /** 无完整知识上下文时的降级 Prompt（保持三段式解析兼容）。 */
    private String buildLegacyPrompt(AiExplainRequestDTO dto) {
        String planDetailsText = dto.getPlanDetails() == null ? "" : dto.getPlanDetails().stream()
                .map(this::formatDetailLine)
                .collect(Collectors.joining("\n"));
        String rulesText = dto.getMatchedRules() == null ? "" : formatRules(dto.getMatchedRules());
        String casesText = dto.getMatchedCases() == null ? "" : formatCases(dto.getMatchedCases());
        String kOrder = StringUtils.hasText(dto.getKnowledgeOrderSummary()) ? dto.getKnowledgeOrderSummary() : "（无）";
        String kInv = StringUtils.hasText(dto.getKnowledgeInventorySummary()) ? dto.getKnowledgeInventorySummary() : "（无）";

        return """
                你是煤矿智能配煤领域专家，请根据以下订单约束、推荐方案、命中规则和历史案例，生成专业解释。

                【订单信息】
                订单编号：%s
                客户名称：%s
                需求量：%s
                灰分要求：%s
                硫分要求：%s
                水分要求：%s
                发热量要求：%s
                优先级：%s

                【知识库—订单与约束摘要】
                %s

                【知识库—候选煤种库存摘录】
                %s

                【推荐方案】
                方案名称：%s
                总成本：%s
                质量评分：%s
                成本评分：%s
                稳定性评分：%s
                综合评分：%s

                【方案明细】
                %s

                【命中规则】
                %s

                【参考案例】
                %s

                输出要求（必须严格遵守）：
                1) 仅输出以下三段，不要复述整段订单/规则原文，不要写长篇推理过程。
                2) 可使用常见 Markdown 增强可读性（如 **加粗**、分段、有序/无序列表），不要使用 HTML 标签。
                3) 每一段控制在约 80~200 字，语言简洁、可执行。

                请按如下格式输出（保留序号与小标题）：
                1. 方案说明：
                2. 风险提示：
                3. 优化建议：

                请使用正式、简洁、专业的中文，不要输出与煤矿配煤无关的内容。
                """.formatted(
                nz(dto.getOrderCode()),
                nz(dto.getCustomerName()),
                fmtNum(dto.getDemandQuantity()),
                fmtNum(dto.getTargetAsh()),
                fmtNum(dto.getTargetSulfur()),
                fmtNum(dto.getTargetMoisture()),
                fmtNum(dto.getTargetCalorific()),
                dto.getPriorityLevel() == null ? "—" : String.valueOf(dto.getPriorityLevel()),
                kOrder,
                kInv,
                nz(dto.getPlanName()),
                fmtNum(dto.getTotalCost()),
                fmtNum(dto.getQualityScore()),
                fmtNum(dto.getCostScore()),
                fmtNum(dto.getStabilityScore()),
                fmtNum(dto.getOverallScore()),
                planDetailsText.isBlank() ? "（无）" : planDetailsText,
                rulesText.isBlank() ? "（无）" : rulesText,
                casesText.isBlank() ? "（无）" : casesText
        );
    }

    private static String nzBlock(String s) {
        return StringUtils.hasText(s) ? s : "（无）";
    }

    private static String blankToEmpty(String s) {
        return s == null ? "" : s;
    }

    private String formatDetailLine(PlanDetailVO d) {
        String name = d.getCoalName() == null ? ("煤种ID" + d.getCoalId()) : d.getCoalName();
        BigDecimal pct = d.getBlendRatio() == null ? null : d.getBlendRatio().multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
        return String.format(
                "%s：配比%s%%，使用量%s吨，预测硫分%s%%，预测热值%s，单价%s元/吨",
                name,
                pct == null ? "—" : pct.toPlainString(),
                fmtNum(d.getUseQuantity()),
                fmtNum(d.getPredictedSulfur()),
                fmtNum(d.getPredictedCalorific()),
                fmtNum(d.getUnitCost())
        );
    }

    private String formatRules(List<MatchedRuleVO> rules) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rules.size(); i++) {
            MatchedRuleVO r = rules.get(i);
            String line = r.getRuleName() == null ? r.getRuleCode() : r.getRuleName();
            String content = r.getRuleContent() == null ? "" : r.getRuleContent();
            sb.append("规则").append(i + 1).append("：").append(line).append("（").append(nz(r.getRuleType())).append("）");
            if (StringUtils.hasText(r.getHitReason())) {
                sb.append("；命中原因：").append(r.getHitReason());
            }
            if (!content.isBlank()) {
                sb.append("——").append(content);
            }
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    private String formatCases(List<MatchedCaseVO> cases) {
        if (cases == null || cases.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cases.size(); i++) {
            MatchedCaseVO c = cases.get(i);
            String title = c.getCaseName() == null ? c.getCaseCode() : c.getCaseName();
            String desc = c.getSummary() == null ? "" : c.getSummary();
            sb.append("案例").append(i + 1).append("：").append(title);
            if (StringUtils.hasText(c.getMatchReason())) {
                sb.append("；匹配原因：").append(c.getMatchReason());
            }
            if (!desc.isBlank()) {
                sb.append("——").append(desc);
            }
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    private static String nz(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }

    private static String fmtNum(BigDecimal v) {
        return v == null ? "—" : v.stripTrailingZeros().toPlainString();
    }
}
