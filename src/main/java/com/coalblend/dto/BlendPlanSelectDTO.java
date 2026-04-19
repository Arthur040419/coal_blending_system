package com.coalblend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BlendPlanSelectDTO {

    @NotNull(message = "planId 不能为空")
    private Long planId;
}
