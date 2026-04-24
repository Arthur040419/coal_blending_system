package com.coalblend.service.intelligent.model;

import com.coalblend.entity.BlendPlanDetail;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class EvaluatedPlanDraft {

    private List<Long> coalIds;
    private List<BigDecimal> ratios;
    private List<BlendPlanDetail> details;
    private BigDecimal totalCost;
    private ConstraintResult constraintResult;
    private ScoreDetail scoreDetail;
    private String explanation;
    private String riskTip;
}
