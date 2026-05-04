package com.coalblend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.coalblend.vo.blend.DecisionProblemItemVO;
import com.coalblend.vo.blend.DecisionSuggestionItemVO;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("blend_plan")
public class BlendPlan {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String planCode;
    private Long orderId;
    private String planName;
    private BigDecimal totalCost;
    private BigDecimal qualityScore;
    private BigDecimal costScore;
    private BigDecimal stabilityScore;
    private BigDecimal overallScore;
    /** 是否满足硬约束：1 是，0 否 */
    private Integer feasibleFlag;
    /** 约束校验摘要：预测指标、违反项、风险提示 */
    private String constraintSummary;
    /** 评分明细：质量、成本、库存稳定性和综合评分理由 */
    private String scoreDetail;
    /** 风险等级：low/medium/high */
    private String riskLevel;
    private String planStatus;
    private String explanation;
    /** 知识增强：模型输出的规则/命中依据说明 */
    private String ruleBasis;
    /** RAG JSON输出：案例参考 */
    private String caseReference;
    /** RAG JSON输出：推荐理由 */
    private String recommendReason;
    /** RAG JSON输出：最终解释 */
    private String finalExplanation;
    private String riskTip;
    /** AI 优化建议 */
    private String optimizeSuggestion;
    /** 解释所用模型名称 */
    private String aiModelName;
    /** 是否由大模型生成：1 是，0 否 */
    private Integer aiGenerateFlag;
    /** 方案执行后形成的最终产品批次号 */
    private String finalProductBatchNo;
    /** 追溯状态：not_executed/executed/inspected/shipped */
    private String traceStatus;
    /** 候选来源：system/ai/hybrid */
    private String candidateSource;
    /** AI候选生成理由 */
    private String aiCandidateReason;
    private String decisionStatus;
    private String recommendationMode;
    private String scoreStrategy;
    private Integer paretoRank;
    private BigDecimal objectiveCostPerTon;
    private BigDecimal objectiveQualityDeviation;
    private BigDecimal objectiveExecutionRisk;
    private String problemItemsJson;
    private String suggestionItemsJson;
    private String generationConfigJson;
    @TableField(exist = false)
    private List<DecisionProblemItemVO> problemItems;
    @TableField(exist = false)
    private List<DecisionSuggestionItemVO> suggestionItems;
    @TableField(exist = false)
    private String decisionStatusLabel;
    @TableField(exist = false)
    private String recommendationModeLabel;
    private Long createBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
