package com.coalblend.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.coalblend.entity.ExperimentRecord;
import com.coalblend.vo.experiment.ExperimentRadarVO;

import java.util.List;

public interface ExperimentRecordService {

    void saveRecord(ExperimentRecord record);

    IPage<ExperimentRecord> page(long current, long size, String experimentCode, Long orderId,
                                 String modelName, Long planId);

    List<ExperimentRecord> listByOrder(Long orderId);

    ExperimentRadarVO radar(String experimentCode, Long orderId, String modelName);
}
