package com.coalblend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("raw_coal_batch")
public class RawCoalBatch {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String rawBatchNo;
    private Long sourceId;
    private Long coalId;
    private LocalDate productionDate;
    private String shiftNo;
    private BigDecimal outputQuantity;
    private BigDecimal gangueRate;
    private String destination;
    private String warehouseCode;
    private String status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
