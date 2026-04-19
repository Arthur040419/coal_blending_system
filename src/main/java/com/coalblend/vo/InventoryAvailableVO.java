package com.coalblend.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryAvailableVO {

    private Long coalId;
    private BigDecimal totalAvailableQuantity;
    private List<WarehouseLine> warehouses;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WarehouseLine {
        private String warehouseCode;
        private BigDecimal availableQuantity;
    }
}
