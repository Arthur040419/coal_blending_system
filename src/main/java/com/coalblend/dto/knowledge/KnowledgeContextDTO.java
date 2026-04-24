package com.coalblend.dto.knowledge;

import com.coalblend.vo.knowledge.MatchedCaseVO;
import com.coalblend.vo.knowledge.MatchedRuleVO;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 知识增强大模型用的统一上下文（订单/方案/规则/案例/库存文本 + 结构化列表）。
 */
@Data
public class KnowledgeContextDTO {

    private String orderSummary;
    private String inventorySummary;
    private String coalSummary;
    /** 推荐方案一句话摘要 */
    private String planSummary;
    private List<MatchedRuleVO> matchedRules = new ArrayList<>();
    private List<MatchedCaseVO> matchedCases = new ArrayList<>();

    private String orderText;
    /** 推荐方案全文块（方案书式，供 Prompt {planText}） */
    private String planText;
    /** 方案明细纯文本（可并入 planText，此字段便于调试与扩展） */
    private String planDetailsText;
    private String inventoryText;
    private String rulesText;
    private String casesText;

    // --- 结构化订单关键字段（与论文设计一致，可选用于扩展/RAG）---
    private String orderCode;
    private String customerName;
    private BigDecimal demandQuantity;
    private BigDecimal targetAsh;
    private BigDecimal targetSulfur;
    private BigDecimal targetMoisture;
    private BigDecimal targetCalorific;
    private Integer priorityLevel;
    private LocalDate deliveryDate;
}
