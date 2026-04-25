package com.coalblend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("batch_lineage")
public class BatchLineage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String parentBatchNo;
    private String parentBatchType;
    private String childBatchNo;
    private String childBatchType;
    private String processStage;
    private BigDecimal quantity;
    private BigDecimal ratio;
    private LocalDateTime operationTime;
    private String operatorName;
    private String remark;
    private LocalDateTime createTime;
}
