package com.coalblend.service.intelligent;

import com.coalblend.enums.ScoreStrategyType;
import com.coalblend.entity.Orders;
import com.coalblend.service.intelligent.model.EvaluatedPlanDraft;
import com.coalblend.service.intelligent.model.BlendGenerationRuntimeConfig;
import com.coalblend.service.intelligent.model.PlanCoalSnapshot;

import java.math.BigDecimal;
import java.util.List;

public interface PlanScoreService {

    EvaluatedPlanDraft evaluate(Orders order, List<PlanCoalSnapshot> snapshots, List<BigDecimal> ratios);

    EvaluatedPlanDraft evaluate(Orders order, List<PlanCoalSnapshot> snapshots, List<BigDecimal> ratios,
                                ScoreStrategyType scoreStrategy, BlendGenerationRuntimeConfig runtimeConfig);
}
