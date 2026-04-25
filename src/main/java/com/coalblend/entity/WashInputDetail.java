package com.coalblend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("wash_input_detail")
public class WashInputDetail {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long washBatchId;
    private Long rawBatchId;
    private BigDecimal inputQuantity;
    private BigDecimal inputRatio;
    private String qualitySnapshotJson;
    private String remark;
    private LocalDateTime createTime;
}
