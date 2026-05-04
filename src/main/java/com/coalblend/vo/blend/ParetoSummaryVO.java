package com.coalblend.vo.blend;

import lombok.Data;

@Data
public class ParetoSummaryVO {
    private Integer totalCandidateCount;
    private Integer feasibleCount;
    private Integer riskyCount;
    private Integer infeasibleCount;
    private Integer paretoFrontCount;
    private String bestDecisionStatus;
    private String bestPlanName;
}
