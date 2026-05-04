package com.coalblend.service.intelligent;

import com.coalblend.entity.Orders;
import com.coalblend.service.intelligent.model.BlendGenerationRuntimeConfig;
import com.coalblend.service.intelligent.model.EvaluatedPlanDraft;

import java.util.List;

public interface PlanDecisionService {

    void decide(Orders order, List<EvaluatedPlanDraft> drafts, BlendGenerationRuntimeConfig runtimeConfig);
}
