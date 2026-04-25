package com.coalblend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coalblend.common.exception.BusinessException;
import com.coalblend.common.result.Result;
import com.coalblend.entity.*;
import com.coalblend.mapper.*;
import com.coalblend.service.chain.BatchLineageService;
import com.coalblend.service.chain.BatchNoGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shipmentDelivery")
@RequiredArgsConstructor
public class ShipmentDeliveryController {

    private final ShipmentDeliveryMapper shipmentMapper;
    private final ProductBatchMapper productBatchMapper;
    private final OrdersMapper ordersMapper;
    private final BlendPlanMapper blendPlanMapper;
    private final BatchNoGenerator batchNoGenerator;
    private final BatchLineageService lineageService;

    @GetMapping("/page")
    public Result<IPage<ShipmentDelivery>> page(@RequestParam(defaultValue = "1") long current,
                                                @RequestParam(defaultValue = "10") long size,
                                                @RequestParam(required = false) Long orderId,
                                                @RequestParam(required = false) Long productBatchId) {
        LambdaQueryWrapper<ShipmentDelivery> w = new LambdaQueryWrapper<>();
        if (orderId != null) w.eq(ShipmentDelivery::getOrderId, orderId);
        if (productBatchId != null) w.eq(ShipmentDelivery::getProductBatchId, productBatchId);
        w.orderByDesc(ShipmentDelivery::getId);
        return Result.ok(shipmentMapper.selectPage(new Page<>(current, size), w));
    }

    @PostMapping("/add")
    @Transactional(rollbackFor = Exception.class)
    public Result<ShipmentDelivery> add(@RequestBody ShipmentDelivery body) {
        ProductBatch product = productBatchMapper.selectById(body.getProductBatchId());
        if (product == null) throw new BusinessException("产品批次不存在");
        if (body.getOrderId() == null) body.setOrderId(product.getOrderId());
        if (body.getOrderId() == null) throw new BusinessException("订单ID不能为空");
        if (!StringUtils.hasText(body.getShipmentNo())) body.setShipmentNo(batchNoGenerator.shipmentNo());
        if (!StringUtils.hasText(body.getDeliveryStatus())) body.setDeliveryStatus("shipped");
        shipmentMapper.insert(body);

        lineageService.record(product.getProductBatchNo(), "final_product", body.getShipmentNo(), "shipment",
                "shipment", body.getShipmentQuantity(), null, null, "最终产品发运交付");
        productBatchMapper.update(null, new LambdaUpdateWrapper<ProductBatch>()
                .eq(ProductBatch::getId, product.getId())
                .set(ProductBatch::getStatus, "shipped"));
        if (product.getPlanId() != null) {
            blendPlanMapper.update(null, new LambdaUpdateWrapper<BlendPlan>()
                    .eq(BlendPlan::getId, product.getPlanId())
                    .set(BlendPlan::getTraceStatus, "shipped"));
        }
        Orders orderPatch = new Orders();
        orderPatch.setId(body.getOrderId());
        orderPatch.setOrderStatus("completed");
        ordersMapper.updateById(orderPatch);
        return Result.ok(body);
    }

    @GetMapping("/byOrder/{orderId}")
    public Result<List<ShipmentDelivery>> byOrder(@PathVariable Long orderId) {
        return Result.ok(shipmentMapper.selectList(new LambdaQueryWrapper<ShipmentDelivery>()
                .eq(ShipmentDelivery::getOrderId, orderId)
                .orderByDesc(ShipmentDelivery::getId)));
    }

    @GetMapping("/byProduct/{productBatchId}")
    public Result<List<ShipmentDelivery>> byProduct(@PathVariable Long productBatchId) {
        return Result.ok(shipmentMapper.selectList(new LambdaQueryWrapper<ShipmentDelivery>()
                .eq(ShipmentDelivery::getProductBatchId, productBatchId)
                .orderByDesc(ShipmentDelivery::getId)));
    }
}
