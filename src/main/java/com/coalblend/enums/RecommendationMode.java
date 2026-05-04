package com.coalblend.enums;

import lombok.Getter;

@Getter
public enum RecommendationMode {
    NORMAL("已生成可执行推荐方案"),
    RISK_REFERENCE("当前无完全可行方案，返回风险最小参考方案"),
    NO_SOLUTION("当前约束下无法生成可执行方案");

    private final String label;

    RecommendationMode(String label) {
        this.label = label;
    }
}
