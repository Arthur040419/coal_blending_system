package com.coalblend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("shipment_delivery")
public class ShipmentDelivery {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String shipmentNo;
    private Long orderId;
    private Long productBatchId;
    private String customerName;
    private BigDecimal shipmentQuantity;
    private String vehicleNo;
    private LocalDateTime loadingTime;
    private LocalDateTime deliveryTime;
    private String deliveryStatus;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
