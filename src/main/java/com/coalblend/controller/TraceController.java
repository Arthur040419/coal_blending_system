package com.coalblend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.coalblend.common.result.Result;
import com.coalblend.entity.BatchLineage;
import com.coalblend.entity.BlendPlan;
import com.coalblend.entity.ProductBatch;
import com.coalblend.mapper.BlendPlanMapper;
import com.coalblend.mapper.OrdersMapper;
import com.coalblend.mapper.ProductBatchMapper;
import com.coalblend.service.chain.BatchLineageService;
import com.coalblend.vo.chain.ChainTraceVO;
import com.coalblend.vo.chain.TraceNodeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/trace")
@RequiredArgsConstructor
public class TraceController {

    private final BatchLineageService lineageService;
    private final BlendPlanMapper blendPlanMapper;
    private final ProductBatchMapper productBatchMapper;
    private final OrdersMapper ordersMapper;

    @GetMapping("/upstream/{batchNo}")
    public Result<List<BatchLineage>> upstream(@PathVariable String batchNo) {
        return Result.ok(lineageService.upstream(batchNo));
    }

    @GetMapping("/downstream/{batchNo}")
    public Result<List<BatchLineage>> downstream(@PathVariable String batchNo) {
        return Result.ok(lineageService.downstream(batchNo));
    }

    @GetMapping("/tree/{batchNo}")
    public Result<TraceNodeVO> tree(@PathVariable String batchNo,
                                    @RequestParam(defaultValue = "batch") String batchType) {
        return Result.ok(lineageService.upstreamTree(batchNo, batchType));
    }

    @GetMapping("/byBatch/{batchNo}")
    public Result<ChainTraceVO> byBatch(@PathVariable String batchNo,
                                        @RequestParam(defaultValue = "batch") String batchType) {
        ChainTraceVO vo = new ChainTraceVO();
        vo.setUpstream(lineageService.upstream(batchNo));
        vo.setDownstream(lineageService.downstream(batchNo));
        vo.setUpstreamTree(lineageService.upstreamTree(batchNo, batchType));
        return Result.ok(vo);
    }

    @GetMapping("/byPlan/{planId}")
    public Result<ChainTraceVO> byPlan(@PathVariable Long planId) {
        BlendPlan plan = blendPlanMapper.selectById(planId);
        ChainTraceVO vo = new ChainTraceVO();
        vo.setPlan(plan);
        if (plan != null) {
            vo.setOrder(ordersMapper.selectById(plan.getOrderId()));
            ProductBatch product = productBatchMapper.selectOne(new LambdaQueryWrapper<ProductBatch>()
                    .eq(ProductBatch::getProductBatchNo, plan.getFinalProductBatchNo())
                    .last("LIMIT 1"));
            vo.setFinalProduct(product);
            if (product != null) {
                vo.setUpstream(lineageService.upstream(product.getProductBatchNo()));
                vo.setDownstream(lineageService.downstream(product.getProductBatchNo()));
                vo.setUpstreamTree(lineageService.upstreamTree(product.getProductBatchNo(), "final_product"));
            }
        }
        return Result.ok(vo);
    }

    @GetMapping("/byOrder/{orderId}")
    public Result<ChainTraceVO> byOrder(@PathVariable Long orderId) {
        BlendPlan plan = blendPlanMapper.selectOne(new LambdaQueryWrapper<BlendPlan>()
                .eq(BlendPlan::getOrderId, orderId)
                .isNotNull(BlendPlan::getFinalProductBatchNo)
                .orderByDesc(BlendPlan::getId)
                .last("LIMIT 1"));
        if (plan == null) {
            ChainTraceVO empty = new ChainTraceVO();
            empty.setOrder(ordersMapper.selectById(orderId));
            return Result.ok(empty);
        }
        return byPlan(plan.getId());
    }
}
