package com.coalblend.enums;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public enum ScoreStrategyType {
    BALANCED("均衡策略", new BigDecimal("0.50"), new BigDecimal("0.20"), new BigDecimal("0.30")),
    QUALITY_FIRST("质量优先", new BigDecimal("0.65"), new BigDecimal("0.15"), new BigDecimal("0.20")),
    COST_FIRST("成本优先", new BigDecimal("0.40"), new BigDecimal("0.40"), new BigDecimal("0.20")),
    INVENTORY_FIRST("库存优先", new BigDecimal("0.40"), new BigDecimal("0.15"), new BigDecimal("0.45"));

    private final String label;
    private final BigDecimal qualityWeight;
    private final BigDecimal costWeight;
    private final BigDecimal stabilityWeight;

    ScoreStrategyType(String label, BigDecimal qualityWeight, BigDecimal costWeight, BigDecimal stabilityWeight) {
        this.label = label;
        this.qualityWeight = qualityWeight;
        this.costWeight = costWeight;
        this.stabilityWeight = stabilityWeight;
    }

    public static ScoreStrategyType parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            return BALANCED;
        }
        for (ScoreStrategyType item : values()) {
            if (item.name().equalsIgnoreCase(value.trim())) {
                return item;
            }
        }
        return BALANCED;
    }
}
