package com.coalblend.service.intelligent;

import com.coalblend.entity.ModelConfig;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.Locale;
import java.util.Set;

/**
 * Shared validation and endpoint helpers for model_config records used by LLM calls.
 */
public final class LlmConfigSupport {

    private static final Set<String> USABLE_MODEL_TYPES = Set.of("LLM", "LOCAL_OLLAMA", "OLLAMA");

    private LlmConfigSupport() {
    }

    public static boolean isUsableLlmConfig(ModelConfig cfg) {
        return !StringUtils.hasText(unusableReason(cfg));
    }

    public static String unusableReason(ModelConfig cfg) {
        if (cfg == null) {
            return "model_config 不存在";
        }
        if (!Integer.valueOf(1).equals(cfg.getStatus())) {
            return "model_config.status 不是 1";
        }
        if (!USABLE_MODEL_TYPES.contains(normalizeType(cfg.getModelType()))) {
            return "model_config.model_type 不是 LLM/LOCAL_OLLAMA";
        }
        if (!StringUtils.hasText(cfg.getApiUrl())) {
            return "model_config.api_url 为空";
        }
        return "";
    }

    public static boolean isOllamaNativeChatUrl(String url) {
        return StringUtils.hasText(url) && url.contains("/api/chat");
    }

    public static boolean isOllamaNativeGenerateUrl(String url) {
        return StringUtils.hasText(url) && url.contains("/api/generate");
    }

    public static String endpointLabel(String url) {
        if (!StringUtils.hasText(url)) {
            return "";
        }
        try {
            URI uri = URI.create(url.trim());
            if (!StringUtils.hasText(uri.getScheme()) || !StringUtils.hasText(uri.getHost())) {
                return url.trim();
            }
            StringBuilder sb = new StringBuilder();
            sb.append(uri.getScheme()).append("://").append(uri.getHost());
            if (uri.getPort() > 0) {
                sb.append(':').append(uri.getPort());
            }
            if (StringUtils.hasText(uri.getPath())) {
                sb.append(uri.getPath());
            }
            return sb.toString();
        } catch (Exception ignored) {
            return url.trim();
        }
    }

    private static String normalizeType(String modelType) {
        return modelType == null ? "" : modelType.trim().toUpperCase(Locale.ROOT);
    }
}
