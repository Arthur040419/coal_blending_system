package com.coalblend.vo;

import lombok.Data;

/**
 * 一次解释生成的完整结果（含是否来自真实模型调用）。
 */
@Data
public class AiExplainResultVO {

    private String explanation;
    private String ruleBasis;
    private String riskTip;
    private String optimizeSuggestion;
    private boolean aiGenerated;
    private String modelNameUsed;
    private String rawText;
}
