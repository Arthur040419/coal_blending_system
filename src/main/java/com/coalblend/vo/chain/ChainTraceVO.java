package com.coalblend.vo.chain;

import com.coalblend.entity.BatchLineage;
import com.coalblend.entity.BlendPlan;
import com.coalblend.entity.Orders;
import com.coalblend.entity.ProductBatch;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ChainTraceVO {
    private Orders order;
    private BlendPlan plan;
    private ProductBatch finalProduct;
    private TraceNodeVO upstreamTree;
    private List<BatchLineage> upstream = new ArrayList<>();
    private List<BatchLineage> downstream = new ArrayList<>();
}
