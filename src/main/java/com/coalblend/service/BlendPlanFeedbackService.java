package com.coalblend.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.coalblend.entity.BlendPlanFeedback;

import java.util.List;

public interface BlendPlanFeedbackService {

    IPage<BlendPlanFeedback> page(long current, long size, Long planId, Long orderId, Integer qualifiedFlag);

    List<BlendPlanFeedback> listByPlan(Long planId);

    BlendPlanFeedback getById(Long id);

    void add(BlendPlanFeedback feedback);

    void update(BlendPlanFeedback feedback);

    Long convertToCase(Long feedbackId);
}
