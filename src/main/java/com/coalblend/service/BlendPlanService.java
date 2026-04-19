package com.coalblend.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.coalblend.dto.BlendGenerateDTO;
import com.coalblend.entity.BlendPlan;
import com.coalblend.entity.BlendPlanDetail;
import com.coalblend.vo.blend.BlendGenerateResultVO;

import java.util.List;

public interface BlendPlanService {

    IPage<BlendPlan> page(long current, long size, Long orderId, String planStatus, String planCode,
                          String orderCode, String createTimeBegin, String createTimeEnd);

    BlendPlan getById(Long id);

    List<BlendPlanDetail> listDetails(Long planId);

    List<BlendPlan> listByOrder(Long orderId);

    void selectPlan(Long planId);

    BlendGenerateResultVO generate(BlendGenerateDTO dto);
}
