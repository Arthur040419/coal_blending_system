package com.coalblend.service.intelligent.impl;

import com.coalblend.common.config.BlendOptimizationProperties;
import com.coalblend.common.exception.BusinessException;
import com.coalblend.dto.BlendGenerateDTO;
import com.coalblend.enums.ScoreStrategyType;
import com.coalblend.service.intelligent.BlendGenerationConfigResolver;
import com.coalblend.service.intelligent.model.BlendGenerationRuntimeConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class BlendGenerationConfigResolverImpl implements BlendGenerationConfigResolver {

    private final BlendOptimizationProperties properties;

    @Override
    public BlendGenerationRuntimeConfig resolve(BlendGenerateDTO dto) {
        BlendGenerationRuntimeConfig config = new BlendGenerationRuntimeConfig();
        config.setScoreStrategy(ScoreStrategyType.parse(dto == null ? null : dto.getScoreStrategy()));
        config.setRatioStep(resolveRatioStep(dto == null ? null : dto.getRatioStep()));
        config.setMaxShortlistCoals(resolveRange(dto == null ? null : dto.getMaxShortlistCoals(),
                properties.getMaxShortlistCoals(), 3, 10, "maxShortlistCoals"));
        config.setMaxMaterialCount(resolveMaterialCount(dto == null ? null : dto.getMaxMaterialCount()));
        config.setMaxReturnPlans(resolveRange(dto == null ? null : dto.getMaxReturnPlans(),
                properties.getMaxReturnPlans(), 3, 10, "maxReturnPlans"));
        config.setMaxEvaluatedCandidates(properties.getMaxEvaluatedCandidates());
        config.setMinSingleRatio(properties.getMinSingleRatio());
        config.setHighSingleRatioThreshold(properties.getHighSingleRatioThreshold());
        config.setLowInventoryMarginRate(properties.getLowInventoryMarginRate());
        config.setLowQualityMarginRate(properties.getLowQualityMarginRate());
        config.setLowCalorificMarginRate(properties.getLowCalorificMarginRate());
        return config;
    }

    private BigDecimal resolveRatioStep(BigDecimal input) {
        BigDecimal value = input == null ? properties.getRatioStep() : input;
        if (value == null) {
            value = new BigDecimal("0.05");
        }
        BigDecimal normalized = value.stripTrailingZeros();
        if (normalized.compareTo(new BigDecimal("0.05")) != 0
                && normalized.compareTo(new BigDecimal("0.10")) != 0) {
            throw new BusinessException("ratioStep 只能为 0.05 或 0.10");
        }
        return normalized.setScale(2);
    }

    private Integer resolveMaterialCount(Integer input) {
        Integer value = input == null ? properties.getMaxMaterialCount() : input;
        if (value == null) {
            value = 3;
        }
        if (value != 2 && value != 3) {
            throw new BusinessException("maxMaterialCount 只能为 2 或 3");
        }
        return value;
    }

    private Integer resolveRange(Integer input, Integer defaultValue, int min, int max, String field) {
        int value = input == null ? (defaultValue == null ? min : defaultValue) : input;
        if (value < min) {
            throw new BusinessException(field + " 不能小于 " + min);
        }
        if (value > max) {
            throw new BusinessException(field + " 不能大于 " + max);
        }
        return value;
    }
}
