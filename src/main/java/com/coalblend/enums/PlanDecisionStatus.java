package com.coalblend.enums;

import lombok.Getter;

@Getter
public enum PlanDecisionStatus {
    FEASIBLE("可执行", 0),
    RISKY("风险参考", 1),
    INFEASIBLE("不可执行", 2);

    private final String label;
    private final int priority;

    PlanDecisionStatus(String label, int priority) {
        this.label = label;
        this.priority = priority;
    }
}
