package com.coalblend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderStatusDTO {

    @NotNull(message = "id 不能为空")
    private Long id;

    @NotBlank(message = "orderStatus 不能为空")
    private String orderStatus;
}
