package com.coalblend.service.intelligent.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.coalblend.common.config.CoalLlmProperties;
import com.coalblend.dto.AiExplainRequestDTO;
import com.coalblend.dto.knowledge.KnowledgeContextDTO;
import com.coalblend.entity.ModelConfig;
import com.coalblend.entity.Orders;
import com.coalblend.entity.BlendPlan;
import com.coalblend.mapper.BlendPlanMapper;
import com.coalblend.mapper.ModelConfigMapper;
import com.coalblend.service.intelligent.ModelInferenceService;
import com.coalblend.service.intelligent.PromptBuildService;
import com.coalblend.vo.AiExplainResponseVO;
import com.coalblend.vo.AiExplainResultVO;
import com.coalblend.vo.blend.PlanWithDetailsVO;
import com.coalblend.vo.knowledge.MatchedCaseVO;
import com.coalblend.vo.knowledge.MatchedRuleVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ModelInferenceServiceImpl implements ModelInferenceService {

    private final ModelConfigMapper modelConfigMapper;
    private final BlendPlanMapper blendPlanMapper;
    private final PromptBuildService promptBuildService;
    private final CoalLlmProperties coalLlmProperties;
    private final ObjectMapper objectMapper;
    private final RestTemplate llmRestTemplate;

    public ModelInferenceServiceImpl(
            ModelConfigMapper modelConfigMapper,
            BlendPlanMapper blendPlanMapper,
            PromptBuildService promptBuildService,
            CoalLlmProperties coalLlmProperties,
            ObjectMapper objectMapper,
            @Qualifier("llmRestTemplate") RestTemplate llmRestTemplate) {
        this.modelConfigMapper = modelConfigMapper;
        this.blendPlanMapper = blendPlanMapper;
        this.promptBuildService = promptBuildService;
        this.coalLlmProperties = coalLlmProperties;
        this.objectMapper = objectMapper;
        this.llmRestTemplate = llmRestTemplate;
    }

    @Override
    public AiExplainResultVO enrichRecommendedPlan(Long recommendedPlanId, Orders order, PlanWithDetailsVO recommended,
                                                   List<MatchedRuleVO> matchedRules, List<MatchedCaseVO> matchedCases,
                                                   KnowledgeContextDTO knowledgeContext, Long modelConfigId) {
        AiExplainRequestDTO req = promptBuildService.buildRequest(order, recommended, matchedRules, matchedCases,
                knowledgeContext);
        String prompt = promptBuildService.buildPrompt(req, knowledgeContext);

        ModelConfig cfg = loadModelConfig(modelConfigId);
        if (!coalLlmProperties.isEnabled() || cfg == null || !StringUtils.hasText(cfg.getApiUrl())) {
            log.info("Skip LLM call (enabled={}, configPresent={})", coalLlmProperties.isEnabled(), cfg != null);
            AiExplainResultVO fallback = persistFallback(recommendedPlanId, cfg, "未启用大模型或未配置有效 model_config");
            fallback.setPromptText(prompt);
            return fallback;
        }

        try {
            String raw = callChatCompletions(cfg, prompt);
            AiExplainResponseVO parsed = parseModelOutput(raw);
            if (!StringUtils.hasText(parsed.getExplanation())
                    && !StringUtils.hasText(parsed.getRuleBasis())
                    && !StringUtils.hasText(parsed.getCaseReference())
                    && !StringUtils.hasText(parsed.getRecommendReason())
                    && !StringUtils.hasText(parsed.getRiskTip())
                    && !StringUtils.hasText(parsed.getOptimizeSuggestion())) {
                log.warn("LLM returned empty sections, use fallback");
                AiExplainResultVO fallback = persistFallback(recommendedPlanId, cfg, "模型输出为空");
                fallback.setPromptText(prompt);
                return fallback;
            }
            AiExplainResultVO result = persistAiResult(recommendedPlanId, cfg.getModelName(), parsed, raw);
            result.setPromptText(prompt);
            return result;
        } catch (RestClientException e) {
            log.warn("LLM HTTP error: {}", e.getMessage());
            AiExplainResultVO fallback = persistFallback(recommendedPlanId, cfg, "HTTP: " + e.getMessage());
            fallback.setPromptText(prompt);
            return fallback;
        } catch (Exception e) {
            log.warn("LLM invoke failed", e);
            AiExplainResultVO fallback = persistFallback(recommendedPlanId, cfg, e.getClass().getSimpleName());
            fallback.setPromptText(prompt);
            return fallback;
        }
    }

    private ModelConfig loadModelConfig(Long modelConfigId) {
        if (modelConfigId != null) {
            ModelConfig cfg = modelConfigMapper.selectById(modelConfigId);
            return isUsableLlmConfig(cfg) ? cfg : null;
        }
        return modelConfigMapper.selectOne(new LambdaQueryWrapper<ModelConfig>()
                .eq(ModelConfig::getStatus, 1)
                .in(ModelConfig::getModelType, List.of("LLM", "LOCAL_OLLAMA"))
                .isNotNull(ModelConfig::getApiUrl)
                .ne(ModelConfig::getApiUrl, "")
                .orderByDesc(ModelConfig::getId)
                .last("LIMIT 1"));
    }

    private boolean isUsableLlmConfig(ModelConfig cfg) {
        return cfg != null
                && Integer.valueOf(1).equals(cfg.getStatus())
                && List.of("LLM", "LOCAL_OLLAMA").contains(cfg.getModelType())
                && StringUtils.hasText(cfg.getApiUrl());
    }

    private String callChatCompletions(ModelConfig cfg, String prompt) throws Exception {
        String url = cfg.getApiUrl().trim();
        String modelName = StringUtils.hasText(cfg.getModelName()) ? cfg.getModelName().trim() : "default";

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", modelName);
        body.put("stream", false);
        if (isOllamaNativeChatUrl(url)) {
            // Qwen3/Ollama may otherwise return final text in message.thinking with empty content.
            body.put("think", false);
        }
        ArrayNode messages = body.putArray("messages");
        ObjectNode userMsg = messages.addObject();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);
        if (coalLlmProperties.getMaxTokens() != null && coalLlmProperties.getMaxTokens() > 0) {
            body.put("max_tokens", coalLlmProperties.getMaxTokens());
        }
        if (cfg.getTemperature() != null) {
            body.put("temperature", cfg.getTemperature().doubleValue());
        } else {
            body.put("temperature", 0.5);
        }
        if (cfg.getTopP() != null) {
            body.put("top_p", cfg.getTopP().doubleValue());
        } else {
            body.put("top_p", 0.85);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (StringUtils.hasText(cfg.getApiKey())) {
            headers.setBearerAuth(cfg.getApiKey().trim());
        }
        if (url.contains("openrouter.ai")) {
            headers.add("HTTP-Referer", "http://localhost");
            headers.add("X-Title", "coal-blending-system");
        }
        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);

        ResponseEntity<String> resp = llmRestTemplate.postForEntity(url, entity, String.class);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new IllegalStateException("bad status " + resp.getStatusCode());
        }
        JsonNode root = objectMapper.readTree(resp.getBody());
        String content = extractChatResponseContent(root);
        String finishReason = extractFinishReason(root);
        if ("length".equalsIgnoreCase(finishReason)) {
            log.warn("LLM output may be truncated by max_tokens (finish_reason=length)");
        }
        if (!StringUtils.hasText(content)) {
            String snippet = snippet(resp.getBody());
            log.warn("LLM response has no parsable content. response={}", snippet);
            log.debug("LLM raw response snippet: {}", snippet);
            throw new IllegalStateException("empty model content: " + snippet);
        }
        return content;
    }

    private static boolean isOllamaNativeChatUrl(String url) {
        return StringUtils.hasText(url) && url.contains("/api/chat");
    }

    private static String extractChatResponseContent(JsonNode root) {
        if (root == null || root.isMissingNode() || root.isNull()) {
            return "";
        }

        // OpenAI-compatible Chat Completions: choices[0].message.content
        JsonNode choices = root.path("choices");
        if (choices.isArray() && !choices.isEmpty()) {
            String content = extractContent(choices.get(0));
            if (StringUtils.hasText(content)) {
                return content;
            }
        }

        // Ollama native /api/chat: { "message": { "content": "..." } }
        JsonNode ollamaMessageContent = root.path("message").path("content");
        if (ollamaMessageContent.isTextual() && StringUtils.hasText(ollamaMessageContent.asText())) {
            return ollamaMessageContent.asText();
        }
        JsonNode ollamaThinking = root.path("message").path("thinking");
        if (ollamaThinking.isTextual() && StringUtils.hasText(ollamaThinking.asText())) {
            String jsonText = extractJsonObject(ollamaThinking.asText());
            if (StringUtils.hasText(jsonText)) {
                return jsonText;
            }
        }

        // Ollama native /api/generate: { "response": "..." }
        JsonNode ollamaGenerateResponse = root.path("response");
        if (ollamaGenerateResponse.isTextual() && StringUtils.hasText(ollamaGenerateResponse.asText())) {
            return ollamaGenerateResponse.asText();
        }

        // Some providers expose the final text on the root node.
        String[] rootTextFields = {"output_text", "text", "content"};
        for (String field : rootTextFields) {
            JsonNode n = root.path(field);
            if (n.isTextual() && StringUtils.hasText(n.asText())) {
                return n.asText();
            }
        }

        return "";
    }

    private static String extractFinishReason(JsonNode root) {
        JsonNode choices = root == null ? null : root.path("choices");
        if (choices != null && choices.isArray() && !choices.isEmpty()) {
            return choices.get(0).path("finish_reason").asText("");
        }
        JsonNode doneReason = root == null ? null : root.path("done_reason");
        if (doneReason != null && doneReason.isTextual()) {
            return doneReason.asText("");
        }
        return "";
    }

    private static String snippet(String text) {
        if (text == null) {
            return "";
        }
        return text.length() > 600 ? text.substring(0, 600) + "..." : text;
    }

    private static String extractContent(JsonNode choiceNode) {
        if (choiceNode == null || choiceNode.isMissingNode()) {
            return "";
        }
        JsonNode message = choiceNode.path("message");

        // OpenAI standard: message.content is a plain string.
        JsonNode contentNode = message.path("content");
        if (contentNode.isTextual() && StringUtils.hasText(contentNode.asText())) {
            return contentNode.asText();
        }

        // Some compatible providers return content as an array of content blocks.
        if (contentNode.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode block : contentNode) {
                if (block == null || block.isMissingNode()) {
                    continue;
                }
                JsonNode textNode = block.path("text");
                if (textNode.isTextual() && StringUtils.hasText(textNode.asText())) {
                    if (!sb.isEmpty()) {
                        sb.append('\n');
                    }
                    sb.append(textNode.asText());
                } else if (block.isTextual() && StringUtils.hasText(block.asText())) {
                    if (!sb.isEmpty()) {
                        sb.append('\n');
                    }
                    sb.append(block.asText());
                }
            }
            if (!sb.isEmpty()) {
                return sb.toString();
            }
        }

        // OpenRouter / GLM-like providers may place visible output in these fields.
        String[] fallbackPaths = {
                "reasoning_content",
                "reasoning",
                "output_text",
                "text"
        };
        for (String p : fallbackPaths) {
            JsonNode n = message.path(p);
            if (n.isTextual() && StringUtils.hasText(n.asText())) {
                return n.asText();
            }
            JsonNode top = choiceNode.path(p);
            if (top.isTextual() && StringUtils.hasText(top.asText())) {
                return top.asText();
            }
        }
        return "";
    }

    AiExplainResponseVO parseModelOutput(String text) {
        AiExplainResponseVO vo = new AiExplainResponseVO();
        vo.setRawText(text);
        if (!StringUtils.hasText(text)) {
            return vo;
        }

        AiExplainResponseVO json = parseJsonModelOutput(text);
        if (StringUtils.hasText(json.getExplanation())
                || StringUtils.hasText(json.getRuleBasis())
                || StringUtils.hasText(json.getCaseReference())
                || StringUtils.hasText(json.getRecommendReason())
                || StringUtils.hasText(json.getRiskTip())) {
            return json;
        }

        if (text.contains("规则依据")) {
            String explanation = extractBySectionLabel(text, "方案说明", "规则依据");
            String ruleBasis = extractBySectionLabel(text, "规则依据", "风险提示");
            String risk = extractBySectionLabel(text, "风险提示", "优化建议");
            String opt = extractBySectionLabel(text, "优化建议", null);
            vo.setExplanation(trimSection(explanation));
            vo.setRuleBasis(trimSection(ruleBasis));
            vo.setRiskTip(trimSection(risk));
            vo.setOptimizeSuggestion(trimSection(opt));
            if (StringUtils.hasText(vo.getExplanation()) || StringUtils.hasText(vo.getRuleBasis())
                    || StringUtils.hasText(vo.getRiskTip()) || StringUtils.hasText(vo.getOptimizeSuggestion())) {
                return vo;
            }
        }
        String explanation = extractBySectionLabel(text, "方案说明", "风险提示");
        if (!StringUtils.hasText(explanation)) {
            explanation = extractBetweenOld(text, "方案说明", "风险提示");
        }
        String risk = extractBySectionLabel(text, "风险提示", "优化建议");
        if (!StringUtils.hasText(risk)) {
            risk = extractBetweenOld(text, "风险提示", "优化建议");
        }
        String opt = extractAfterLabel(text, "优化建议");
        if (!StringUtils.hasText(explanation)) {
            explanation = text.trim();
        }
        vo.setExplanation(trimSection(explanation));
        vo.setRuleBasis("");
        vo.setRiskTip(trimSection(risk));
        vo.setOptimizeSuggestion(trimSection(opt));
        return vo;
    }

    private AiExplainResponseVO parseJsonModelOutput(String text) {
        AiExplainResponseVO vo = new AiExplainResponseVO();
        vo.setRawText(text);
        String jsonText = extractJsonObject(text);
        if (!StringUtils.hasText(jsonText)) {
            return vo;
        }
        try {
            JsonNode root = objectMapper.readTree(jsonText);
            vo.setRuleBasis(readText(root, "ruleBasis"));
            vo.setCaseReference(readText(root, "caseReference"));
            vo.setRecommendReason(readText(root, "recommendReason"));
            vo.setRiskTip(readText(root, "riskTip"));
            String finalExplanation = readText(root, "finalExplanation");
            if (!StringUtils.hasText(finalExplanation)) {
                finalExplanation = readText(root, "explanation");
            }
            vo.setExplanation(finalExplanation);
            vo.setOptimizeSuggestion(readText(root, "optimizeSuggestion"));
        } catch (Exception e) {
            log.warn("LLM output is not valid JSON, fallback to section parser: {}", e.getMessage());
            vo.setExplanation(text.trim());
            vo.setRiskTip("模型输出格式不规范，已保留原始解释内容。");
        }
        return vo;
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

    /**
     * 在下一小标题行（可带 1. 2. 、1) 等编号）出现之前，截取本段正文；{@code nextLabel} 为 null 时取到文末。
     */
    private static String extractBySectionLabel(String full, String thisLabel, String nextLabel) {
        Pattern startPat = sectionHeaderPattern(thisLabel);
        Matcher mStart = startPat.matcher(full);
        if (!mStart.find()) {
            return "";
        }
        int bodyStart = mStart.end();
        if (!StringUtils.hasText(nextLabel)) {
            return full.substring(bodyStart).trim();
        }
        Pattern endPat = sectionHeaderPattern(nextLabel);
        Matcher mEnd = endPat.matcher(full);
        if (mEnd.find(bodyStart)) {
            return full.substring(bodyStart, mEnd.start()).trim();
        }
        return full.substring(bodyStart).trim();
    }

    /**
     * 行首为「可选编号 + 小标题 + 冒号」，如：1. 方案说明：、方案说明：、### 方案说明：。
     */
    private static Pattern sectionHeaderPattern(String label) {
        String q = Pattern.quote(label);
        // 行首：可选 #、可选 1. / 2、 等编号、小标题、冒号。冒号后**不再**用 \\s* 吞换行，避免正文起点落到下一行的「2. 」
        return Pattern.compile(
                "^\\s*(?:#+\\s*)?(?:\\d{1,2}[\\.、\\)）]\\s*)?" + q + "\\s*[:：]",
                Pattern.MULTILINE);
    }

    /** 原简单切分，仅作无行首结构时的回退。 */
    private static String extractBetweenOld(String full, String startKeyword, String endKeyword) {
        int i = full.indexOf(startKeyword);
        if (i < 0) {
            return "";
        }
        int c = findColonAfter(full, i);
        int begin = c >= 0 ? Math.min(c + 1, full.length()) : i + startKeyword.length();
        int j = full.indexOf(endKeyword, begin);
        if (j < 0) {
            return full.substring(begin).trim();
        }
        return full.substring(begin, j).trim();
    }

    private static String extractAfterLabel(String full, String label) {
        int p = full.lastIndexOf(label);
        if (p < 0) {
            return "";
        }
        int c = findColonAfter(full, p);
        int begin = c >= p ? c + 1 : p + label.length();
        return full.substring(begin).trim();
    }

    private static int findColonAfter(String s, int from) {
        int a = s.indexOf('：', from);
        int b = s.indexOf(':', from);
        if (a < 0) {
            return b;
        }
        if (b < 0) {
            return a;
        }
        return Math.min(a, b);
    }

    /** 保留 Markdown，仅去掉首尾空白。 */
    private static String trimSection(String input) {
        return input == null ? "" : input.trim();
    }

    private AiExplainResultVO persistAiResult(Long planId, String modelName, AiExplainResponseVO parsed, String raw) {
        blendPlanMapper.update(null, new LambdaUpdateWrapper<BlendPlan>()
                .eq(BlendPlan::getId, planId)
                .set(BlendPlan::getExplanation, parsed.getExplanation())
                .set(BlendPlan::getRuleBasis, nullToEmpty(parsed.getRuleBasis()))
                .set(BlendPlan::getCaseReference, nullToEmpty(parsed.getCaseReference()))
                .set(BlendPlan::getRecommendReason, nullToEmpty(parsed.getRecommendReason()))
                .set(BlendPlan::getFinalExplanation, nullToEmpty(parsed.getExplanation()))
                .set(BlendPlan::getRiskTip, nullToEmpty(parsed.getRiskTip()))
                .set(BlendPlan::getOptimizeSuggestion, nullToEmpty(parsed.getOptimizeSuggestion()))
                .set(BlendPlan::getAiModelName, modelName)
                .set(BlendPlan::getAiGenerateFlag, 1));
        AiExplainResultVO out = new AiExplainResultVO();
        out.setExplanation(parsed.getExplanation());
        out.setRuleBasis(parsed.getRuleBasis());
        out.setCaseReference(parsed.getCaseReference());
        out.setRecommendReason(parsed.getRecommendReason());
        out.setRiskTip(parsed.getRiskTip());
        out.setOptimizeSuggestion(parsed.getOptimizeSuggestion());
        out.setAiGenerated(true);
        out.setModelNameUsed(modelName);
        out.setRawText(raw);
        return out;
    }

    private AiExplainResultVO persistFallback(Long planId, ModelConfig cfg, String reason) {
        log.debug("LLM fallback reason: {}", reason);
        String modelTag = cfg != null && StringUtils.hasText(cfg.getModelName()) ? cfg.getModelName() : "fallback";
        blendPlanMapper.update(null, new LambdaUpdateWrapper<BlendPlan>()
                .eq(BlendPlan::getId, planId)
                .set(BlendPlan::getExplanation, coalLlmProperties.getFallbackExplanation())
                .set(BlendPlan::getRuleBasis, coalLlmProperties.getFallbackRuleBasis())
                .set(BlendPlan::getCaseReference, "当前知识库依据不足，未生成有效案例参考。")
                .set(BlendPlan::getRecommendReason, coalLlmProperties.getFallbackExplanation())
                .set(BlendPlan::getFinalExplanation, coalLlmProperties.getFallbackExplanation())
                .set(BlendPlan::getRiskTip, coalLlmProperties.getFallbackRiskTip())
                .set(BlendPlan::getOptimizeSuggestion, coalLlmProperties.getFallbackOptimizeSuggestion())
                .set(BlendPlan::getAiModelName, modelTag + "(兜底)")
                .set(BlendPlan::getAiGenerateFlag, 0));
        AiExplainResultVO out = new AiExplainResultVO();
        out.setExplanation(coalLlmProperties.getFallbackExplanation());
        out.setRuleBasis(coalLlmProperties.getFallbackRuleBasis());
        out.setCaseReference("当前知识库依据不足，未生成有效案例参考。");
        out.setRecommendReason(coalLlmProperties.getFallbackExplanation());
        out.setRiskTip(coalLlmProperties.getFallbackRiskTip());
        out.setOptimizeSuggestion(coalLlmProperties.getFallbackOptimizeSuggestion());
        out.setAiGenerated(false);
        out.setModelNameUsed(modelTag + "(兜底)");
        out.setRawText(reason);
        return out;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
