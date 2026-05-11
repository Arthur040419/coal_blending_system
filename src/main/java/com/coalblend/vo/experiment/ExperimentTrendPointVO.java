package com.coalblend.vo.experiment;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ExperimentTrendPointVO {

    private Long recordId;
    private String experimentCode;
    private Long orderId;
    private Long planId;
    private String modelName;
    private BigDecimal totalCost;
    private BigDecimal qualityScore;
    private BigDecimal costScore;
    private BigDecimal inventoryScore;
    private BigDecimal finalScore;
    private BigDecimal modelEffectScore;
    private BigDecimal effectiveCandidateRate;
    private Integer aiCandidatePlanCount;
    private Integer acceptedAiCandidateCount;
    private Integer totalCandidateCount;
    private Integer feasibleCandidateCount;
    private Integer llmSuccessFlag;
    private Boolean feasible;
    private LocalDateTime createTime;
}
