package com.coalblend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("blend_plan_feedback")
public class BlendPlanFeedback {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long planId;
    private Long orderId;
    private BigDecimal actualQuantity;
    private BigDecimal actualAsh;
    private BigDecimal actualSulfur;
    private BigDecimal actualMoisture;
    private BigDecimal actualVolatile;
    private BigDecimal actualCalorific;
    private BigDecimal actualCost;
    /** 是否达标：1 是，0 否 */
    private Integer qualifiedFlag;
    /** 执行评价：优秀/良好/一般/较差 */
    private String effectivenessEval;
    private String feedbackDesc;
    private LocalDate executeDate;
    private Long operatorId;
    /** 是否已沉淀为案例：1 是，0 否 */
    private Integer caseGeneratedFlag;
    private Long caseId;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
