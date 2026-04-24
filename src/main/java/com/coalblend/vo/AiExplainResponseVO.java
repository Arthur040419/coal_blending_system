package com.coalblend.vo;

import lombok.Data;

/**
 * 从大模型原始输出解析后的结构化结果。
 */
@Data
public class AiExplainResponseVO {

    private String explanation;
    /** 知识增强：结合命中规则的依据说明 */
    private String ruleBasis;
    private String riskTip;
    private String optimizeSuggestion;
    /** 模型原始文本，便于调试 */
    private String rawText;
}
