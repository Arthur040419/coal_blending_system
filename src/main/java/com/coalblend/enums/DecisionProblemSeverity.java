package com.coalblend.enums;

import lombok.Getter;

@Getter
public enum DecisionProblemSeverity {
    BLOCKER("硬约束违规"),
    WARNING("风险预警");

    private final String label;

    DecisionProblemSeverity(String label) {
        this.label = label;
    }
}
