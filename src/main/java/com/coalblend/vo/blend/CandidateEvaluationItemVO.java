package com.coalblend.vo.blend;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class CandidateEvaluationItemVO {

    private String candidateSource;
    private String planName;
    private String aiCandidateReason;
    private BigDecimal totalCost;
    private BigDecimal qualityScore;
    private BigDecimal costScore;
    private BigDecimal stabilityScore;
    private BigDecimal overallScore;
    private Integer feasibleFlag;
    private String constraintSummary;
    private String scoreDetail;
    private String riskLevel;
    private String riskTip;
    private List<PlanDetailVO> details = new ArrayList<>();

    /** FEASIBLE / RISKY / INFEASIBLE */
    private String decisionStatus;
    private String decisionStatusLabel;
    /** NORMAL / RISK_REFERENCE / NO_SOLUTION，该字段只在最终推荐候选上有值 */
    private String recommendationMode;
    /** 结构化问题项 */
    private List<DecisionProblemItemVO> problemItems = new ArrayList<>();
    /** 结构化建议项 */
    private List<DecisionSuggestionItemVO> suggestionItems = new ArrayList<>();
    /** Pareto 非支配排序等级，1 表示第一前沿 */
    private Integer paretoRank;
    /** 支配该方案的候选数量 */
    private Integer dominatedCount;
    /** 该方案支配的候选数量 */
    private Integer dominatesCount;
    /** 多目标坐标：吨煤成本，越低越好 */
    private BigDecimal objectiveCostPerTon;
    /** 多目标坐标：质量偏差，越低越好 */
    private BigDecimal objectiveQualityDeviation;
    /** 多目标坐标：执行风险，越低越好 */
    private BigDecimal objectiveExecutionRisk;
    /** 本次评分策略 */
    private String scoreStrategy;
}
