package com.coalblend.service.intelligent.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AiBlendCandidateItem {

    private Long coalId;
    private String productBatchNo;
    private BigDecimal ratio;
    private String reason;
}

