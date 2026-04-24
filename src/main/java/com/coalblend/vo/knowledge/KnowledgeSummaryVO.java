package com.coalblend.vo.knowledge;

import lombok.Data;

/**
 * 知识库命中概览（精简字段，便于前端与论文展示）。
 */
@Data
public class KnowledgeSummaryVO {

    private String orderSummary;
    private String inventorySummary;
    /** 推荐方案一句话摘要 */
    private String planSummary;
    private int ruleCount;
    private int caseCount;
}
