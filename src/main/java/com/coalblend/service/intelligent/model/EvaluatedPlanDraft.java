package com.coalblend.service.intelligent.model;

import com.coalblend.entity.BlendPlanDetail;
import com.coalblend.vo.blend.DecisionProblemItemVO;
import com.coalblend.vo.blend.DecisionSuggestionItemVO;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class EvaluatedPlanDraft {

    private List<Long> coalIds;
    private List<BigDecimal> ratios;
    private List<BlendPlanDetail> details;
    private List<PlanCoalSnapshot> snapshots;
    private BigDecimal totalCost;
    private ConstraintResult constraintResult;
    private ScoreDetail scoreDetail;
    private String explanation;
    private String riskTip;
    /** 候选来源：system/ai/hybrid */
    private String candidateSource = "system";
    /** 大模型生成候选方案时给出的策略/理由 */
    private String aiCandidateReason;
    private String decisionStatus;
    private String decisionStatusLabel;
    private String recommendationMode;
    private List<DecisionProblemItemVO> problemItems = new ArrayList<>();
    private List<DecisionSuggestionItemVO> suggestionItems = new ArrayList<>();
    private Integer paretoRank;
    private Integer dominatedCount;
    private Integer dominatesCount;
    private BigDecimal objectiveCostPerTon;
    private BigDecimal objectiveQualityDeviation;
    private BigDecimal objectiveExecutionRisk;
    private String scoreStrategy;
}
