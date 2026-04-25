package com.coalblend.vo;

import lombok.Data;

/**
 * 一次解释生成的完整结果（含是否来自真实模型调用）。
 */
@Data
public class AiExplainResultVO {

    private String explanation;
    private String ruleBasis;
    private String caseReference;
    private String recommendReason;
    private String riskTip;
    private String optimizeSuggestion;
    private boolean aiGenerated;
    private String modelNameUsed;
    /** 本次调用模型使用的最终 Prompt，用于 RAG 追溯日志。 */
    private String promptText;
    private String rawText;
}
