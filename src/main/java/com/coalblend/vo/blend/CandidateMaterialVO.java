package com.coalblend.vo.blend;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CandidateMaterialVO {

    private Integer shortlistRank;
    private String candidateScope;
    private String materialKey;

    private Long coalId;
    private String coalCode;
    private String coalName;
    private String coalCategory;
    private String sourceArea;
    private BigDecimal purchasePrice;

    private Long productBatchId;
    private String productBatchNo;
    private String productBatchName;

    private Long inventoryId;
    private String warehouseCode;
    private String materialStage;
    private String rawBatchNo;
    private BigDecimal stockQuantity;
    private BigDecimal availableQuantity;
    private BigDecimal lockedQuantity;

    private Long qualityId;
    private String qualityBatchNo;
    private String sampleStage;
    private String relatedBatchNo;
    private BigDecimal ashContent;
    private BigDecimal sulfurContent;
    private BigDecimal moistureContent;
    private BigDecimal volatileContent;
    private BigDecimal calorificValue;
    private BigDecimal fixedCarbon;
}
