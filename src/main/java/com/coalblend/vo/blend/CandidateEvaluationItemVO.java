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
}

