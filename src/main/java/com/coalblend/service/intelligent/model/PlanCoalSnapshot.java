package com.coalblend.service.intelligent.model;

import com.coalblend.entity.CoalQuality;
import com.coalblend.entity.CoalType;
import com.coalblend.entity.Inventory;
import lombok.Data;

@Data
public class PlanCoalSnapshot {

    private Long coalId;
    private CoalType type;
    private CoalQuality quality;
    private Inventory inventory;
    /** 产品批次级配煤时记录来源批次 */
    private Long productBatchId;
    private String productBatchNo;
    private String productBatchName;
    private String qualitySnapshotJson;

    public PlanCoalSnapshot(Long coalId, CoalType type, CoalQuality quality, Inventory inventory) {
        this.coalId = coalId;
        this.type = type;
        this.quality = quality;
        this.inventory = inventory;
    }
}
