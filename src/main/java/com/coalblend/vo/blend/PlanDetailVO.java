package com.coalblend.vo.blend;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PlanDetailVO {

    private Long id;
    private Long planId;
    private Long coalId;
    private String coalName;
    private Long productBatchId;
    private String productBatchNo;
    private Long inventoryId;
    private String qualitySnapshotJson;
    private BigDecimal blendRatio;
    private BigDecimal useQuantity;
    private BigDecimal predictedAsh;
    private BigDecimal predictedSulfur;
    private BigDecimal predictedMoisture;
    private BigDecimal predictedVolatile;
    private BigDecimal predictedCalorific;
    private BigDecimal unitCost;
    private String remark;
}
