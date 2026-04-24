package com.coalblend.service.intelligent.model;

import com.coalblend.entity.CoalQuality;
import com.coalblend.entity.CoalType;
import com.coalblend.entity.Inventory;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PlanCoalSnapshot {

    private Long coalId;
    private CoalType type;
    private CoalQuality quality;
    private Inventory inventory;
}
