package com.coalblend.vo.blend;

import lombok.Data;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class BlendPlanExecuteResultVO {
    private String finalProductBatchNo;
    private Long orderId;
    private Long planId;
    private BigDecimal quantity;
    private Map<String, BigDecimal> predictedQuality = new LinkedHashMap<>();
}
