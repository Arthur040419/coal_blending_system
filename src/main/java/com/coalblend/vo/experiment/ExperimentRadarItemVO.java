package com.coalblend.vo.experiment;

import lombok.Data;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class ExperimentRadarItemVO {

    private Long recordId;
    private String experimentCode;
    private Long orderId;
    private Long planId;
    private String modelName;
    private BigDecimal totalCost;
    private BigDecimal avgAsh;
    private BigDecimal avgSulfur;
    private BigDecimal avgMoisture;
    private BigDecimal avgCalorific;
    private Map<String, BigDecimal> radarMetrics = new LinkedHashMap<>();
}
