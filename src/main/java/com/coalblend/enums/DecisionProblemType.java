package com.coalblend.enums;

import lombok.Getter;

@Getter
public enum DecisionProblemType {
    ASH_EXCEED("灰分超标"),
    SULFUR_EXCEED("硫分超标"),
    MOISTURE_EXCEED("水分超标"),
    CALORIFIC_NOT_ENOUGH("发热量不足"),
    VOLATILE_DEVIATION("挥发分偏离"),
    INVENTORY_NOT_ENOUGH("库存不足"),
    INVENTORY_MARGIN_LOW("库存余量偏低"),
    RATIO_SUM_INVALID("配比总和异常"),
    RATIO_SINGLE_TOO_HIGH("单一煤种占比偏高"),
    MATERIAL_COUNT_INVALID("配煤种类数量异常"),
    PRICE_MISSING("煤种价格缺失"),
    QUALITY_MISSING("煤质数据缺失"),
    RULE_VIOLATION("规则约束违反"),
    COST_TOO_HIGH("成本偏高");

    private final String label;

    DecisionProblemType(String label) {
        this.label = label;
    }
}
