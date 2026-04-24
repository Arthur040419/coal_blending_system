package com.coalblend.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 大模型解释生成：超时与兜底文案（可被 model_config 表覆盖调用目标）。
 */
@Data
@ConfigurationProperties(prefix = "coal.llm")
public class CoalLlmProperties {

    /**
     * 是否尝试调用外部大模型（为 false 时直接使用兜底文案，便于无密钥环境开发）。
     */
    private boolean enabled = true;

    private Duration connectTimeout = Duration.ofSeconds(30);
    private Duration readTimeout = Duration.ofMinutes(10);
    /**
     * 单次解释返回 token 上限。小于等于 0 或为空时，不向模型显式传递 max_tokens，
     * 由模型服务自行决定返回长度，避免系统侧主动截断输出。
     */
    private Integer maxTokens = 0;

    private String fallbackExplanation =
            "系统根据订单质量约束、库存情况和规则知识生成了当前推荐方案。该方案综合考虑成本与约束可行性，具有较好的执行可行性。";
    private String fallbackRuleBasis =
            "当前为系统兜底说明：未调用大模型时无法自动生成与命中规则一一对应的依据条文，请在启用模型后重新生成方案。";
    private String fallbackRiskTip = "请关注库存余量与现场执行偏差，以上为系统兜底风险提示。";
    private String fallbackOptimizeSuggestion =
            "可在「模型配置」中填写兼容 OpenAI Chat Completions 的接口地址与密钥后，重新生成以获得大模型细化建议。";
}
