package com.coalblend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("coal_type")
public class CoalType {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String coalCode;
    private String coalName;
    private String coalCategory;
    private String sourceArea;
    private BigDecimal purchasePrice;
    private String transportMode;
    private Integer blendableFlag;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
