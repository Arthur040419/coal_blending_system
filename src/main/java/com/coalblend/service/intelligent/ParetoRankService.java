package com.coalblend.service.intelligent;

import com.coalblend.entity.Orders;
import com.coalblend.service.intelligent.model.EvaluatedPlanDraft;

import java.util.List;

public interface ParetoRankService {

    void rank(Orders order, List<EvaluatedPlanDraft> drafts);
}
