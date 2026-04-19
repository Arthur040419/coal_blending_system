package com.coalblend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ModelConfigStatusDTO {

    @NotNull(message = "id 不能为空")
    private Long id;

    @NotNull(message = "status 不能为空")
    private Integer status;
}
