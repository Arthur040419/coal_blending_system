package com.coalblend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BlendGenerateDTO {

    @NotNull(message = "orderId 不能为空")
    private Long orderId;

    private Long createBy;
}
