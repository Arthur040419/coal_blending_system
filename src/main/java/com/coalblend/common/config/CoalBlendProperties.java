package com.coalblend.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "coal.blend")
public class CoalBlendProperties {

    /**
     * 是否启用系统枚举候选方案。
     * 当前实验阶段可关闭，仅用 AI 候选 + 既有评分标准做优化对比。
     */
    private boolean enableSystemEnumeration = false;
}

