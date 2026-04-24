package com.coalblend.dto;

import com.coalblend.vo.blend.PlanDetailVO;
import com.coalblend.vo.knowledge.MatchedCaseVO;
import com.coalblend.vo.knowledge.MatchedRuleVO;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 大模型解释生成输入（与《系统接入大模型解释生成的实现方案》对齐）。
 */
@Data
public class AiExplainRequestDTO {

    private String orderCode;
    private String customerName;
    private BigDecimal demandQuantity;
    private BigDecimal targetAsh;
    private BigDecimal targetSulfur;
    private BigDecimal targetMoisture;
    private BigDecimal targetCalorific;
    private Integer priorityLevel;

    private String planName;
    private BigDecimal totalCost;
    private BigDecimal qualityScore;
    private BigDecimal costScore;
    private BigDecimal stabilityScore;
    private BigDecimal overallScore;

    private List<PlanDetailVO> planDetails;
    private List<MatchedRuleVO> matchedRules;
    private List<MatchedCaseVO> matchedCases;

    /** 知识组装：订单侧文本（供 Prompt 增强） */
    private String knowledgeOrderSummary;
    /** 知识组装：库存侧文本 */
    private String knowledgeInventorySummary;
}
