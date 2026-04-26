package com.coalblend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coalblend.entity.ExperimentRecord;
import com.coalblend.mapper.ExperimentRecordMapper;
import com.coalblend.service.ExperimentRecordService;
import com.coalblend.vo.experiment.ExperimentRadarItemVO;
import com.coalblend.vo.experiment.ExperimentRadarVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExperimentRecordServiceImpl implements ExperimentRecordService {

    private final ExperimentRecordMapper experimentRecordMapper;

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

    private BigDecimal score(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
