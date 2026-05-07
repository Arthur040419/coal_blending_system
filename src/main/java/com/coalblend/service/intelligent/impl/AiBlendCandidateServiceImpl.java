package com.coalblend.service.intelligent.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.coalblend.common.config.CoalLlmProperties;
import com.coalblend.entity.CoalQuality;
import com.coalblend.entity.CoalType;
import com.coalblend.entity.Inventory;
import com.coalblend.entity.ModelConfig;
import com.coalblend.entity.Orders;
import com.coalblend.mapper.ModelConfigMapper;
import com.coalblend.service.intelligent.AiBlendCandidateService;
import com.coalblend.service.intelligent.LlmConfigSupport;
import com.coalblend.service.intelligent.model.AiBlendCandidateItem;
import com.coalblend.service.intelligent.model.AiBlendCandidatePlan;
import com.coalblend.service.intelligent.model.AiBlendCandidateResult;
import com.coalblend.service.intelligent.model.PlanCoalSnapshot;
import com.coalblend.vo.knowledge.MatchedCaseVO;
import com.coalblend.vo.knowledge.MatchedRuleVO;
import com.coalblend.vo.rag.RagKnowledgeHitVO;
import com.coalblend.vo.rag.RagRetrieveResultVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiBlendCandidateServiceImpl implements AiBlendCandidateService {

    private final ModelConfigMapper modelConfigMapper;
    private final CoalLlmProperties coalLlmProperties;
    private final ObjectMapper objectMapper;
    @Qualifier("llmRestTemplate")
    private final RestTemplate llmRestTemplate;

    @Override
    public AiBlendCandidateResult generateCandidates(Orders order,
                                                     List<PlanCoalSnapshot> candidates,
                                                     List<MatchedRuleVO> matchedRules,
                                                     List<MatchedCaseVO> matchedCases,
                                                     RagRetrieveResultVO ragRetrieveResult,
                                                     String candidateScope,
                                                     Long modelConfigId) {
        AiBlendCandidateResult result = new AiBlendCandidateResult();
        ModelConfig cfg = loadModelConfig(modelConfigId);
        if (!coalLlmProperties.isEnabled() || !LlmConfigSupport.isUsableLlmConfig(cfg)) {
            String reason = !coalLlmProperties.isEnabled() ? "coal.llm.enabled=false" : LlmConfigSupport.unusableReason(cfg);
            log.warn("Skip AI candidate generation LLM call: reason={}, requestedModelConfigId={}, configId={}, name={}, type={}, status={}, hasUrl={}",
                    reason, modelConfigId,
                    cfg == null ? null : cfg.getId(),
                    cfg == null ? null : cfg.getModelName(),
                    cfg == null ? null : cfg.getModelType(),
                    cfg == null ? null : cfg.getStatus(),
                    cfg != null && StringUtils.hasText(cfg.getApiUrl()));
            result.setErrorMessage("未启用大模型或未配置可用 LLM：" + reason);
            return result;
        }
        result.setModelConfigId(cfg.getId());
        result.setModelName(cfg.getModelName());
        String prompt = buildPrompt(order, candidates, matchedRules, matchedCases, ragRetrieveResult, candidateScope);
        try {
            String raw = callModel(cfg, prompt);
            result.setRawText(raw);
            result.setPlans(parsePlans(raw));
            result.setAiGenerated(!result.getPlans().isEmpty());
            log.info("AI candidate generation parsed: modelConfigId={}, planCount={}", cfg.getId(), result.getPlans().size());
            return result;
        } catch (Exception e) {
            log.warn("AI candidate generation failed: {}", e.getMessage(), e);
            result.setErrorMessage(e.getClass().getSimpleName() + ": " + e.getMessage());
            return result;
        }
    }

    private ModelConfig loadModelConfig(Long modelConfigId) {
        if (modelConfigId != null) {
            return modelConfigMapper.selectById(modelConfigId);
        }
        return modelConfigMapper.selectOne(new LambdaQueryWrapper<ModelConfig>()
                .eq(ModelConfig::getStatus, 1)
                .in(ModelConfig::getModelType, List.of("LLM", "LOCAL_OLLAMA", "OLLAMA"))
                .isNotNull(ModelConfig::getApiUrl)
                .ne(ModelConfig::getApiUrl, "")
                .orderByDesc(ModelConfig::getId)
                .last("LIMIT 1"));
    }

    private String buildPrompt(Orders order,
                               List<PlanCoalSnapshot> candidates,
                               List<MatchedRuleVO> matchedRules,
                               List<MatchedCaseVO> matchedCases,
                               RagRetrieveResultVO rag,
                               String candidateScope) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是煤矿智能配煤系统中的候选方案生成助手。请基于订单约束、候选物料、规则和案例，生成候选配比建议。\n");
        sb.append("重要边界：你只负责提出候选配比，系统会再做质量、库存、规则和多目标评分校验。\n\n");
        sb.append("【输出要求】\n");
        sb.append("1. 只能输出 JSON，不要 Markdown，不要解释前缀。\n");
        sb.append("2. JSON 顶层必须为 {\"plans\": [...]}。\n");
        sb.append("3. 输出 3 到 5 个候选方案；无法生成时输出 {\"plans\": []}。\n");
        sb.append("4. 每个方案使用 2 到 4 种候选物料，ratio 之和必须等于 1。\n");
        sb.append("5. 只能使用下方候选物料中的 coalId 和 productBatchNo，不得编造煤种、批次或指标。\n");
        sb.append("6. product_batch 模式下必须填写 productBatchNo；coal_type 模式下 productBatchNo 可为空。\n");
        sb.append("7. 不要修改订单需求量，不要输出库存不足的配比。\n\n");
        sb.append("JSON格式：\n");
        sb.append("{\"plans\":[{\"planName\":\"方案名称\",\"strategy\":\"生成策略\",\"items\":[{\"coalId\":1,\"productBatchNo\":\"PBxxx\",\"ratio\":0.6,\"reason\":\"选择原因\"}],\"risk\":\"风险提示\"}]}\n\n");

        sb.append("【候选范围】").append(candidateScope).append("\n");
        sb.append("【订单信息】\n");
        sb.append("订单编号：").append(order.getOrderCode()).append("\n");
        sb.append("需求量：").append(fmt(order.getDemandQuantity())).append(" 吨\n");
        sb.append("灰分上限：").append(fmt(order.getTargetAsh())).append("%\n");
        sb.append("硫分上限：").append(fmt(order.getTargetSulfur())).append("%\n");
        sb.append("水分上限：").append(fmt(order.getTargetMoisture())).append("%\n");
        sb.append("挥发分参考：").append(fmt(order.getTargetVolatile())).append("%\n");
        sb.append("发热量下限：").append(fmt(order.getTargetCalorific())).append(" kcal/kg\n");
        sb.append("优先级：").append(order.getPriorityLevel()).append("\n\n");

        sb.append("【候选物料】\n");
        for (PlanCoalSnapshot s : candidates) {
            CoalType t = s.getType();
            CoalQuality q = s.getQuality();
            Inventory inv = s.getInventory();
            sb.append("- coalId=").append(s.getCoalId())
                    .append("，productBatchNo=").append(StringUtils.hasText(s.getProductBatchNo()) ? s.getProductBatchNo() : "")
                    .append("，名称=").append(t == null ? "" : t.getCoalName())
                    .append("，可用量=").append(inv == null ? "—" : fmt(inv.getAvailableQuantity())).append("吨")
                    .append("，单价=").append(t == null ? "—" : fmt(t.getPurchasePrice())).append("元/吨")
                    .append("，灰分=").append(q == null ? "—" : fmt(q.getAshContent())).append("%")
                    .append("，硫分=").append(q == null ? "—" : fmt(q.getSulfurContent())).append("%")
                    .append("，水分=").append(q == null ? "—" : fmt(q.getMoistureContent())).append("%")
                    .append("，挥发分=").append(q == null ? "—" : fmt(q.getVolatileContent())).append("%")
                    .append("，发热量=").append(q == null ? "—" : fmt(q.getCalorificValue())).append(" kcal/kg\n");
        }

        sb.append("\n【命中规则】\n");
        matchedRules.stream().limit(6).forEach(r -> sb.append("- ")
                .append(r.getRuleName()).append("：").append(r.getRuleContent())
                .append("；命中原因：").append(r.getHitReason()).append("\n"));

        sb.append("\n【参考案例】\n");
        matchedCases.stream().limit(4).forEach(c -> sb.append("- ")
                .append(c.getCaseName()).append("：").append(c.getSummary())
                .append("；效果：").append(c.getEffectivenessEval())
                .append("；匹配原因：").append(c.getMatchReason()).append("\n"));

        if (rag != null && rag.getAll() != null && !rag.getAll().isEmpty()) {
            sb.append("\n【RAG知识摘录】\n");
            rag.getAll().stream().limit(5).forEach(k -> sb.append("- ")
                    .append(k.getTitle()).append("：").append(shortText(k)).append("\n"));
        }
        return sb.toString();
    }

    private String callModel(ModelConfig cfg, String prompt) throws Exception {
        String url = cfg.getApiUrl().trim();
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", cfg.getModelName());
        body.put("stream", false);
        if (LlmConfigSupport.isOllamaNativeChatUrl(url)) {
            body.put("think", false);
            body.put("format", "json");
        }
        if (LlmConfigSupport.isOllamaNativeGenerateUrl(url)) {
            body.put("prompt", prompt);
            body.put("format", "json");
        } else {
            ArrayNode messages = body.putArray("messages");
            ObjectNode user = messages.addObject();
            user.put("role", "user");
            user.put("content", prompt);
        }
        if (coalLlmProperties.getMaxTokens() != null && coalLlmProperties.getMaxTokens() > 0) {
            body.put("max_tokens", coalLlmProperties.getMaxTokens());
        }
        body.put("temperature", cfg.getTemperature() == null ? 0.35 : cfg.getTemperature().doubleValue());
        body.put("top_p", cfg.getTopP() == null ? 0.85 : cfg.getTopP().doubleValue());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (StringUtils.hasText(cfg.getApiKey())) {
            headers.setBearerAuth(cfg.getApiKey().trim());
        }
        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
        log.info("Calling LLM candidate endpoint: modelConfigId={}, modelName={}, modelType={}, url={}",
                cfg.getId(), cfg.getModelName(), cfg.getModelType(), LlmConfigSupport.endpointLabel(url));
        ResponseEntity<String> resp = llmRestTemplate.postForEntity(url, entity, String.class);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new IllegalStateException("bad status " + resp.getStatusCode());
        }
        JsonNode root = objectMapper.readTree(resp.getBody());
        String content = extractChatResponseContent(root);
        if (!StringUtils.hasText(content)) {
            throw new IllegalStateException("empty model content: " + snippet(resp.getBody()));
        }
        return content;
    }

    private List<AiBlendCandidatePlan> parsePlans(String raw) throws Exception {
        String jsonText = extractJsonObject(raw);
        if (!StringUtils.hasText(jsonText)) {
            return List.of();
        }
        JsonNode root = objectMapper.readTree(jsonText);
        JsonNode plansNode = root.isArray() ? root : root.path("plans");
        if (!plansNode.isArray()) {
            return List.of();
        }
        List<AiBlendCandidatePlan> plans = new java.util.ArrayList<>();
        for (JsonNode p : plansNode) {
            AiBlendCandidatePlan plan = new AiBlendCandidatePlan();
            plan.setPlanName(readText(p, "planName"));
            plan.setStrategy(readText(p, "strategy"));
            plan.setRisk(readText(p, "risk"));
            JsonNode items = p.path("items");
            if (items.isArray()) {
                for (JsonNode itemNode : items) {
                    AiBlendCandidateItem item = new AiBlendCandidateItem();
                    item.setCoalId(readLong(itemNode, "coalId"));
                    item.setProductBatchNo(readText(itemNode, "productBatchNo"));
                    item.setRatio(readDecimal(itemNode, "ratio"));
                    item.setReason(readText(itemNode, "reason"));
                    plan.getItems().add(item);
                }
            }
            plans.add(plan);
        }
        return plans;
    }

    private static String extractChatResponseContent(JsonNode root) {
        JsonNode choices = root.path("choices");
        if (choices.isArray() && !choices.isEmpty()) {
            String content = readText(choices.get(0).path("message"), "content");
            if (StringUtils.hasText(content)) {
                return content;
            }
            content = readText(choices.get(0), "text");
            if (StringUtils.hasText(content)) {
                return content;
            }
        }
        String content = readText(root.path("message"), "content");
        if (StringUtils.hasText(content)) {
            return content;
        }
        String thinking = readText(root.path("message"), "thinking");
        if (StringUtils.hasText(thinking)) {
            String json = extractJsonObject(thinking);
            if (StringUtils.hasText(json)) {
                return json;
            }
        }
        content = readText(root, "response");
        if (StringUtils.hasText(content)) {
            return content;
        }
        content = readText(root, "output_text");
        if (StringUtils.hasText(content)) {
            return content;
        }
        return readText(root, "text");
    }

    private static String readText(JsonNode root, String field) {
        JsonNode n = root == null ? null : root.path(field);
        if (n == null || n.isMissingNode() || n.isNull()) {
            return "";
        }
        if (n.isTextual()) {
            return n.asText().trim();
        }
        return n.toString();
    }

    private static Long readLong(JsonNode root, String field) {
        JsonNode n = root == null ? null : root.path(field);
        if (n == null || n.isMissingNode() || n.isNull()) {
            return null;
        }
        if (n.isNumber()) {
            return n.asLong();
        }
        String text = n.asText("");
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return Long.parseLong(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static BigDecimal readDecimal(JsonNode root, String field) {
        JsonNode n = root == null ? null : root.path(field);
        if (n == null || n.isMissingNode() || n.isNull()) {
            return null;
        }
        try {
            return new BigDecimal(n.asText().trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static String extractJsonObject(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String s = text.trim();
        if (s.startsWith("```")) {
            s = s.replaceFirst("^```(?:json)?\\s*", "");
            s = s.replaceFirst("\\s*```$", "");
        }
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return "";
        }
        return s.substring(start, end + 1);
    }

    private static String shortText(RagKnowledgeHitVO k) {
        String content = k.getContent() == null ? "" : k.getContent().trim();
        return content.length() > 160 ? content.substring(0, 160) + "..." : content;
    }

    private static String snippet(String text) {
        if (text == null) {
            return "";
        }
        return text.length() > 500 ? text.substring(0, 500) + "..." : text;
    }

    private static String fmt(BigDecimal value) {
        return value == null ? "—" : value.stripTrailingZeros().toPlainString();
    }
}
