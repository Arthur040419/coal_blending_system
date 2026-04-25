package com.coalblend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("wash_process_batch")
public class WashProcessBatch {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String washBatchNo;
    private String processType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal feedQuantity;
    private BigDecimal cleanCoalYield;
    private BigDecimal middlingsYield;
    private BigDecimal slimeYield;
    private BigDecimal gangueYield;
    private BigDecimal mediumDensity;
    private BigDecimal separationDensity;
    private String equipmentCode;
    private String operatorName;
    private String status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
