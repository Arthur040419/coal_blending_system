package com.coalblend.service.intelligent.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AiBlendCandidatePlan {

    private String planName;
    private String strategy;
    private List<AiBlendCandidateItem> items = new ArrayList<>();
    private String risk;
}

