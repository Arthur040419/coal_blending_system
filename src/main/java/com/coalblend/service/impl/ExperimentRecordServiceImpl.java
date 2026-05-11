package com.coalblend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coalblend.entity.ExperimentRecord;
import com.coalblend.mapper.ExperimentRecordMapper;
import com.coalblend.service.ExperimentRecordService;
import com.coalblend.vo.experiment.ExperimentModelEffectVO;
import com.coalblend.vo.experiment.ExperimentRadarItemVO;
import com.coalblend.vo.experiment.ExperimentRadarVO;
import com.coalblend.vo.experiment.ExperimentTrendPointVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExperimentRecordServiceImpl implements ExperimentRecordService {

    private final ExperimentRecordMapper experimentRecordMapper;
    private final ObjectMapper objectMapper;

    @Override
    public void saveRecord(ExperimentRecord record) {
        experimentRecordMapper.insert(record);
    }

    @Override
    public IPage<ExperimentRecord> page(long current, long size, String experimentCode, Long orderId,
                                        String modelName, Long planId) {
        Page<ExperimentRecord> page = new Page<>(current, size);
        return experimentRecordMapper.selectPage(page, buildQuery(experimentCode, orderId, modelName, planId));
    }

    @Override
    public List<ExperimentRecord> listByOrder(Long orderId) {
        return experimentRecordMapper.selectList(buildQuery(null, orderId, null, null));
    }

    @Override
    public ExperimentRadarVO radar(String experimentCode, Long orderId, String modelName) {
        List<ExperimentRecord> records = experimentRecordMapper.selectList(
                buildQuery(experimentCode, orderId, modelName, null));
        ExperimentRadarVO vo = new ExperimentRadarVO();
        vo.setExperimentCode(experimentCode);
        vo.setOrderId(orderId);
        vo.setModelName(modelName);
        vo.setItems(records.stream().map(this::toRadarItem).collect(Collectors.toList()));
        return vo;
    }

    @Override
    public List<ExperimentModelEffectVO> modelEffect(String modelName, Long orderId) {
        List<ExperimentRecord> records = experimentRecordMapper.selectList(buildQuery(null, orderId, modelName, null));
        Map<String, List<ExperimentRecord>> byModel = records.stream()
                .filter(r -> StringUtils.hasText(r.getModelName()))
                .collect(Collectors.groupingBy(ExperimentRecord::getModelName, LinkedHashMap::new, Collectors.toList()));
        return byModel.entrySet().stream()
                .map(e -> toModelEffect(e.getKey(), e.getValue()))
                .sorted(Comparator
                        .comparing(ExperimentModelEffectVO::getAvgModelEffectScore, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ExperimentModelEffectVO::getAvgFinalScore, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ExperimentModelEffectVO::getExperimentCount, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ExperimentModelEffectVO::getModelName))
                .collect(Collectors.toList());
    }

    private LambdaQueryWrapper<ExperimentRecord> buildQuery(String experimentCode, Long orderId,
                                                           String modelName, Long planId) {
        LambdaQueryWrapper<ExperimentRecord> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(experimentCode)) {
            w.eq(ExperimentRecord::getExperimentCode, experimentCode);
        }
        if (orderId != null) {
            w.eq(ExperimentRecord::getOrderId, orderId);
        }
        if (StringUtils.hasText(modelName)) {
            w.eq(ExperimentRecord::getModelName, modelName);
        }
        if (planId != null) {
            w.eq(ExperimentRecord::getPlanId, planId);
        }
        w.orderByDesc(ExperimentRecord::getCreateTime).orderByDesc(ExperimentRecord::getId);
        return w;
    }

    private ExperimentRadarItemVO toRadarItem(ExperimentRecord r) {
        ExperimentRadarItemVO vo = new ExperimentRadarItemVO();
        vo.setRecordId(r.getId());
        vo.setExperimentCode(r.getExperimentCode());
        vo.setOrderId(r.getOrderId());
        vo.setPlanId(r.getPlanId());
        vo.setModelName(r.getModelName());
        vo.setTotalCost(r.getTotalCost());
        vo.setAvgAsh(r.getAvgAsh());
        vo.setAvgSulfur(r.getAvgSulfur());
        vo.setAvgMoisture(r.getAvgMoisture());
        vo.setAvgCalorific(r.getAvgCalorific());
        vo.getRadarMetrics().put("质量匹配", score(r.getQualityScore()));
        vo.getRadarMetrics().put("成本优势", score(r.getCostScore()));
        vo.getRadarMetrics().put("库存合理", score(r.getInventoryScore()));
        vo.getRadarMetrics().put("综合效果", score(r.getFinalScore()));
        return vo;
    }

    private ExperimentModelEffectVO toModelEffect(String modelName, List<ExperimentRecord> records) {
        List<ExperimentRecord> rows = records == null ? List.of() : records;
        List<ExperimentRecord> bestByExperiment = bestRecordsByExperiment(rows);
        ExperimentRecord best = bestByExperiment.stream()
                .max(Comparator
                        .comparing(ExperimentRecord::getFinalScore, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ExperimentRecord::getCreateTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);

        ExperimentModelEffectVO vo = new ExperimentModelEffectVO();
        vo.setModelName(modelName);
        vo.setPlanCount(rows.size());
        vo.setExperimentCount(bestByExperiment.size());
        vo.setFeasibleExperimentCount((int) bestByExperiment.stream().filter(this::isFeasible).count());
        vo.setFeasibleRate(rate(vo.getFeasibleExperimentCount(), vo.getExperimentCount()));
        vo.setLlmSuccessExperimentCount((int) bestByExperiment.stream().filter(this::llmSuccess).count());
        vo.setGenerationSuccessRate(rate(vo.getLlmSuccessExperimentCount(), vo.getExperimentCount()));
        vo.setEffectiveCandidateRate(avgValue(bestByExperiment.stream()
                .map(this::effectiveCandidateRate)
                .collect(Collectors.toList())));
        vo.setAvgModelEffectScore(avgValue(bestByExperiment.stream()
                .map(this::modelEffectScore)
                .collect(Collectors.toList())));
        vo.setAvgTotalCost(avg(bestByExperiment, ExperimentRecord::getTotalCost));
        vo.setAvgQualityScore(avg(bestByExperiment, ExperimentRecord::getQualityScore));
        vo.setAvgCostScore(avg(bestByExperiment, ExperimentRecord::getCostScore));
        vo.setAvgInventoryScore(avg(bestByExperiment, ExperimentRecord::getInventoryScore));
        vo.setAvgFinalScore(avg(bestByExperiment, ExperimentRecord::getFinalScore));
        vo.setBestFinalScore(best == null ? null : best.getFinalScore());
        vo.setBestExperimentCode(best == null ? null : best.getExperimentCode());
        vo.setBestPlanId(best == null ? null : best.getPlanId());
        vo.setFirstCreateTime(rows.stream().map(ExperimentRecord::getCreateTime).filter(Objects::nonNull)
                .min(LocalDateTime::compareTo).orElse(null));
        vo.setLastCreateTime(rows.stream().map(ExperimentRecord::getCreateTime).filter(Objects::nonNull)
                .max(LocalDateTime::compareTo).orElse(null));
        vo.getRadarMetrics().put("方案质量", score(vo.getAvgFinalScore()));
        vo.getRadarMetrics().put("可执行性", score(percentScore(vo.getFeasibleRate())));
        vo.getRadarMetrics().put("候选有效", score(percentScore(vo.getEffectiveCandidateRate())));
        vo.getRadarMetrics().put("生成稳定", score(percentScore(vo.getGenerationSuccessRate())));
        vo.getRadarMetrics().put("模型效果", score(vo.getAvgModelEffectScore()));
        vo.getTrend().addAll(bestByExperiment.stream()
                .sorted(Comparator.comparing(ExperimentRecord::getCreateTime, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ExperimentRecord::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toTrendPoint)
                .collect(Collectors.toList()));
        return vo;
    }

    private List<ExperimentRecord> bestRecordsByExperiment(List<ExperimentRecord> records) {
        Map<String, ExperimentRecord> map = new LinkedHashMap<>();
        for (ExperimentRecord record : records) {
            String key = experimentKey(record);
            ExperimentRecord current = map.get(key);
            if (current == null || compareExperimentRecord(record, current) > 0) {
                map.put(key, record);
            }
        }
        return new ArrayList<>(map.values());
    }

    private int compareExperimentRecord(ExperimentRecord a, ExperimentRecord b) {
        int scoreCmp = nz(a.getFinalScore()).compareTo(nz(b.getFinalScore()));
        if (scoreCmp != 0) {
            return scoreCmp;
        }
        LocalDateTime at = a.getCreateTime();
        LocalDateTime bt = b.getCreateTime();
        if (at != null && bt != null) {
            int timeCmp = at.compareTo(bt);
            if (timeCmp != 0) {
                return timeCmp;
            }
        }
        return Long.compare(a.getId() == null ? 0L : a.getId(), b.getId() == null ? 0L : b.getId());
    }

    private String experimentKey(ExperimentRecord record) {
        if (StringUtils.hasText(record.getExperimentCode())) {
            return record.getExperimentCode();
        }
        return "record-" + record.getId();
    }

    private ExperimentTrendPointVO toTrendPoint(ExperimentRecord r) {
        ExperimentTrendPointVO vo = new ExperimentTrendPointVO();
        vo.setRecordId(r.getId());
        vo.setExperimentCode(r.getExperimentCode());
        vo.setOrderId(r.getOrderId());
        vo.setPlanId(r.getPlanId());
        vo.setModelName(r.getModelName());
        vo.setTotalCost(r.getTotalCost());
        vo.setQualityScore(r.getQualityScore());
        vo.setCostScore(r.getCostScore());
        vo.setInventoryScore(r.getInventoryScore());
        vo.setFinalScore(r.getFinalScore());
        vo.setModelEffectScore(modelEffectScore(r));
        vo.setEffectiveCandidateRate(effectiveCandidateRate(r));
        vo.setAiCandidatePlanCount(r.getAiCandidatePlanCount());
        vo.setAcceptedAiCandidateCount(r.getAcceptedAiCandidateCount());
        vo.setTotalCandidateCount(r.getTotalCandidateCount());
        vo.setFeasibleCandidateCount(r.getFeasibleCandidateCount());
        vo.setLlmSuccessFlag(r.getLlmSuccessFlag());
        vo.setFeasible(isFeasible(r));
        vo.setCreateTime(r.getCreateTime());
        return vo;
    }

    private BigDecimal avg(List<ExperimentRecord> rows, java.util.function.Function<ExperimentRecord, BigDecimal> getter) {
        return avgValue(rows.stream().map(getter).filter(Objects::nonNull).collect(Collectors.toList()));
    }

    private BigDecimal avgValue(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal rate(Integer count, Integer total) {
        if (count == null || total == null || total <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(count)
                .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP);
    }

    private boolean isFeasible(ExperimentRecord record) {
        if (!StringUtils.hasText(record.getConstraintHit())) {
            return true;
        }
        try {
            JsonNode root = objectMapper.readTree(record.getConstraintHit());
            JsonNode feasible = root.path("feasible");
            return feasible.isMissingNode() || feasible.asBoolean(true);
        } catch (Exception e) {
            return true;
        }
    }

    private boolean llmSuccess(ExperimentRecord record) {
        if (record.getLlmSuccessFlag() != null) {
            return record.getLlmSuccessFlag() == 1;
        }
        return record.getAcceptedAiCandidateCount() != null && record.getAcceptedAiCandidateCount() > 0
                || record.getPlanId() != null;
    }

    private BigDecimal effectiveCandidateRate(ExperimentRecord record) {
        if (record.getEffectiveCandidateRate() != null) {
            return record.getEffectiveCandidateRate();
        }
        Integer returned = record.getAiCandidatePlanCount();
        Integer accepted = record.getAcceptedAiCandidateCount();
        if (returned == null || returned <= 0 || accepted == null) {
            return record.getPlanId() == null ? BigDecimal.ZERO : BigDecimal.ONE;
        }
        return BigDecimal.valueOf(accepted)
                .divide(BigDecimal.valueOf(returned), 4, RoundingMode.HALF_UP)
                .min(BigDecimal.ONE);
    }

    private BigDecimal modelEffectScore(ExperimentRecord record) {
        if (record.getModelEffectScore() != null) {
            return record.getModelEffectScore();
        }
        BigDecimal planScore = score(record.getFinalScore());
        BigDecimal feasibleScore = isFeasible(record) ? new BigDecimal("100") : BigDecimal.ZERO;
        BigDecimal effectiveScore = effectiveCandidateRate(record).multiply(new BigDecimal("100"));
        BigDecimal successScore = llmSuccess(record) ? new BigDecimal("100") : BigDecimal.ZERO;
        return planScore.multiply(new BigDecimal("0.50"))
                .add(feasibleScore.multiply(new BigDecimal("0.25")))
                .add(effectiveScore.multiply(new BigDecimal("0.15")))
                .add(successScore.multiply(new BigDecimal("0.10")))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal percentScore(BigDecimal rate) {
        return rate == null ? BigDecimal.ZERO : rate.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal score(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
