package com.coalblend.service.intelligent.model;

import com.coalblend.enums.ScoreStrategyType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BlendGenerationRuntimeConfig {
    private ScoreStrategyType scoreStrategy;
    private BigDecimal ratioStep;
    private Integer maxShortlistCoals;
    private Integer maxMaterialCount;
    private Integer maxReturnPlans;
    private Integer maxEvaluatedCandidates;
    private BigDecimal minSingleRatio;
    private BigDecimal highSingleRatioThreshold;
    private BigDecimal lowInventoryMarginRate;
    private BigDecimal lowQualityMarginRate;
    private BigDecimal lowCalorificMarginRate;
}
