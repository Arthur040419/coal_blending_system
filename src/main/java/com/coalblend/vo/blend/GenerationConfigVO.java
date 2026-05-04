package com.coalblend.vo.blend;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class GenerationConfigVO {
    private String scoreStrategy;
    private String scoreStrategyLabel;
    private BigDecimal qualityWeight;
    private BigDecimal costWeight;
    private BigDecimal stabilityWeight;
    private BigDecimal ratioStep;
    private Integer maxShortlistCoals;
    private Integer maxMaterialCount;
    private Integer maxReturnPlans;
}
