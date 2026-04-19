package com.coalblend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("blend_plan_detail")
public class BlendPlanDetail {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long planId;
    private Long coalId;
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
