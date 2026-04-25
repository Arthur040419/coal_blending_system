package com.coalblend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("product_batch")
public class ProductBatch {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String productBatchNo;
    private Long washBatchId;
    private Long coalId;
    private Long orderId;
    private Long planId;
    private String productType;
    private String productName;
    private BigDecimal quantity;
    private BigDecimal availableQuantity;
    private String warehouseCode;
    private BigDecimal ashContent;
    private BigDecimal sulfurContent;
    private BigDecimal moistureContent;
    private BigDecimal volatileContent;
    private BigDecimal calorificValue;
    private String status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
