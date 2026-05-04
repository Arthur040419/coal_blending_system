package com.coalblend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BlendGenerateDTO {

    @NotNull(message = "orderId 不能为空")
    private Long orderId;

    private Long createBy;

    /**
     * 候选物料范围：coal_type（默认煤种级）/ product_batch（洗后产品批次级）/ mixed（预留）。
     */
    private String candidateScope;

    /**
     * 模型调优效果对比实验编号。为空时后端自动生成；传入相同编号可对比不同模型的生成效果。
     */
    private String experimentCode;

    /** 评分策略：BALANCED / QUALITY_FIRST / COST_FIRST / INVENTORY_FIRST */
    private String scoreStrategy;

    /** 配比步长，只接受 0.05 或 0.10 */
    private BigDecimal ratioStep;

    /** 候选煤种短名单数量，取值 3-10 */
    private Integer maxShortlistCoals;

    /** 最大配煤种类数，取值 2-3 */
    private Integer maxMaterialCount;

    /** 返回前端的落库候选方案数量，取值 3-10 */
    private Integer maxReturnPlans;
}
