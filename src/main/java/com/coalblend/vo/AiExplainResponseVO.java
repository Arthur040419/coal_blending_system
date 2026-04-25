package com.coalblend.vo;

import lombok.Data;

/**
 * 从大模型原始输出解析后的结构化结果。
 */
@Data
public class AiExplainResponseVO {

    /** JSON字段：finalExplanation；兼容旧字段名 explanation */
    private String explanation;
    /** 知识增强：结合命中规则的依据说明 */
    private String ruleBasis;
    /** JSON字段：caseReference */
    private String caseReference;
    /** JSON字段：recommendReason */
    private String recommendReason;
    private String riskTip;
    /** 兼容旧四段式字段；JSON模式下通常为空 */
    private String optimizeSuggestion;
    /** 模型原始文本，便于调试 */
    private String rawText;
}
