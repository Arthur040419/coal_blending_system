package com.coalblend.vo.experiment;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class ExperimentModelEffectVO {

    private String modelName;
    /** 按 modelName + experimentCode 聚合后的实验次数。 */
    private Integer experimentCount;
    /** 该模型历史保存的候选方案记录数。 */
    private Integer planCount;
    private Integer feasibleExperimentCount;
    private BigDecimal feasibleRate;
    private Integer llmSuccessExperimentCount;
    private BigDecimal generationSuccessRate;
    private BigDecimal effectiveCandidateRate;
    private BigDecimal avgModelEffectScore;
    private BigDecimal avgTotalCost;
    private BigDecimal avgQualityScore;
    private BigDecimal avgCostScore;
    private BigDecimal avgInventoryScore;
    private BigDecimal avgFinalScore;
    private BigDecimal bestFinalScore;
    private String bestExperimentCode;
    private Long bestPlanId;
    private LocalDateTime firstCreateTime;
    private LocalDateTime lastCreateTime;
    private Map<String, BigDecimal> radarMetrics = new LinkedHashMap<>();
    private List<ExperimentTrendPointVO> trend = new ArrayList<>();
}
