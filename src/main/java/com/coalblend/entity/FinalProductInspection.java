package com.coalblend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("final_product_inspection")
public class FinalProductInspection {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String reportNo;
    private Long productBatchId;
    private Long orderId;
    private Long planId;
    private LocalDateTime sampleTime;
    private String samplePoint;
    private BigDecimal ashContent;
    private BigDecimal sulfurContent;
    private BigDecimal moistureContent;
    private BigDecimal volatileContent;
    private BigDecimal calorificValue;
    private Integer qualifiedFlag;
    private String inspector;
    private String standardBasis;
    private String remark;
    private LocalDateTime createTime;
}
