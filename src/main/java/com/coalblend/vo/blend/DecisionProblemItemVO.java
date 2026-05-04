package com.coalblend.vo.blend;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DecisionProblemItemVO {
    private String type;
    private String typeLabel;
    private String severity;
    private String severityLabel;
    private String fieldName;
    private String message;
    private BigDecimal actualValue;
    private BigDecimal targetValue;
    private BigDecimal deviation;
    private String unit;
    private Long coalId;
    private String coalName;
}
