package com.coalblend.vo.blend;

import com.coalblend.entity.CaseSample;
import com.coalblend.entity.Orders;
import com.coalblend.entity.RuleKnowledge;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class BlendGenerateResultVO {

    private Orders order;
    private Map<String, Object> constraints = new LinkedHashMap<>();
    private PlanWithDetailsVO recommendedPlan;
    private List<PlanWithDetailsVO> candidatePlans;
    private List<RuleKnowledge> matchedRules;
    private List<CaseSample> matchedCases;
    private String explainSummary;
}
