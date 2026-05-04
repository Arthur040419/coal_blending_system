package com.coalblend.service.intelligent.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ScoreDetail {

    private BigDecimal qualityScore;
    private BigDecimal costScore;
    private BigDecimal stabilityScore;
    private BigDecimal overallScore;
    private String qualityReason;
    private String costReason;
    private String stabilityReason;
    private String overallReason;
    private String scoreStrategy;
    private BigDecimal qualityWeight;
    private BigDecimal costWeight;
    private BigDecimal stabilityWeight;
}
