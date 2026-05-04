package com.coalblend.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Data
@Component
@ConfigurationProperties(prefix = "coal.blend.optimization")
public class BlendOptimizationProperties {
    private BigDecimal ratioStep = new BigDecimal("0.05");
    private Integer maxShortlistCoals = 8;
    private Integer maxMaterialCount = 3;
    private Integer maxReturnPlans = 6;
    private Integer maxEvaluatedCandidates = 25000;
    private BigDecimal minSingleRatio = new BigDecimal("0.05");
    private BigDecimal highSingleRatioThreshold = new BigDecimal("0.75");
    private BigDecimal lowInventoryMarginRate = new BigDecimal("0.10");
    private BigDecimal lowQualityMarginRate = new BigDecimal("0.05");
    private BigDecimal lowCalorificMarginRate = new BigDecimal("0.02");
}
