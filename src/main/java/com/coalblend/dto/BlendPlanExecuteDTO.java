package com.coalblend.dto;

import lombok.Data;

@Data
public class BlendPlanExecuteDTO {
    private Long planId;
    private String operatorName;
    private String warehouseCode;
    private String remark;
}
