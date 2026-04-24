package com.coalblend.service.intelligent;

import com.coalblend.entity.Orders;
import com.coalblend.service.intelligent.model.EvaluatedPlanDraft;
import com.coalblend.service.intelligent.model.PlanCoalSnapshot;

import java.math.BigDecimal;
import java.util.List;

public interface PlanScoreService {

    EvaluatedPlanDraft evaluate(Orders order, List<PlanCoalSnapshot> snapshots, List<BigDecimal> ratios);
}
