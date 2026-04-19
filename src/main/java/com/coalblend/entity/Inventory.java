package com.coalblend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("inventory")
public class Inventory {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long coalId;
    private String warehouseCode;
    private BigDecimal stockQuantity;
    private BigDecimal availableQuantity;
    private LocalDateTime updateTime;
    private Integer status;
    private String remark;
}
