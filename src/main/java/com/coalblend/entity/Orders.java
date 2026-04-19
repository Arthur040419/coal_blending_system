package com.coalblend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("orders")
public class Orders {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderCode;
    private String customerName;
    private BigDecimal demandQuantity;
    private BigDecimal targetAsh;
    private BigDecimal targetSulfur;
    private BigDecimal targetMoisture;
    private BigDecimal targetVolatile;
    private BigDecimal targetCalorific;
    private Integer priorityLevel;
    private LocalDate deliveryDate;
    private String orderStatus;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
