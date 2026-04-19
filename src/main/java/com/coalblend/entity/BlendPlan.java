package com.coalblend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("blend_plan")
public class BlendPlan {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String planCode;
    private Long orderId;
    private String planName;
    private BigDecimal totalCost;
    private BigDecimal qualityScore;
    private BigDecimal costScore;
    private BigDecimal stabilityScore;
    private BigDecimal overallScore;
    private String planStatus;
    private String explanation;
    private String riskTip;
    private Long createBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
