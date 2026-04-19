package com.coalblend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("coal_quality")
public class CoalQuality {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long coalId;
    private String batchNo;
    private LocalDateTime sampleTime;
    private BigDecimal ashContent;
    private BigDecimal sulfurContent;
    private BigDecimal moistureContent;
    private BigDecimal volatileContent;
    private BigDecimal calorificValue;
    private BigDecimal fixedCarbon;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
