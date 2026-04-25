package com.coalblend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("mine_source")
public class MineSource {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String sourceCode;
    private String mineArea;
    private String mineName;
    private String coalSeam;
    private String workingFace;
    private String coalCategory;
    private BigDecimal designedCapacity;
    private BigDecimal geologicalAsh;
    private BigDecimal geologicalSulfur;
    private BigDecimal geologicalMoisture;
    private BigDecimal geologicalVolatile;
    private BigDecimal geologicalCalorific;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
