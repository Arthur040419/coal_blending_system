package com.coalblend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("experiment_record")
public class ExperimentRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String experimentCode;
    private Long orderId;
    private String modelName;
    private Long planId;
    private BigDecimal totalCost;
    private BigDecimal avgAsh;
    private BigDecimal avgSulfur;
    private BigDecimal avgMoisture;
    private BigDecimal avgCalorific;
    private BigDecimal qualityScore;
    private BigDecimal costScore;
    private BigDecimal inventoryScore;
    private BigDecimal finalScore;
    private String constraintHit;
    private String riskWarning;
    private String explainText;
    private LocalDateTime createTime;
}
