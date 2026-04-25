package com.coalblend.service.intelligent.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AiBlendCandidateResult {

    private boolean aiGenerated;
    private String modelName;
    private String rawText;
    private String errorMessage;
    private List<AiBlendCandidatePlan> plans = new ArrayList<>();
}

