package com.coalblend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BlendGenerateDTO {

    @NotNull(message = "orderId 不能为空")
    private Long orderId;

    private Long createBy;

    /**
     * 候选物料范围：coal_type（默认煤种级）/ product_batch（洗后产品批次级）/ mixed（预留）。
     */
    private String candidateScope;
}
