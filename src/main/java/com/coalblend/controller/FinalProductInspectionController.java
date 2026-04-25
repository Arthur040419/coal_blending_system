package com.coalblend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coalblend.common.exception.BusinessException;
import com.coalblend.common.result.Result;
import com.coalblend.entity.BlendPlan;
import com.coalblend.entity.CoalQuality;
import com.coalblend.entity.FinalProductInspection;
import com.coalblend.entity.ProductBatch;
import com.coalblend.mapper.BlendPlanMapper;
import com.coalblend.mapper.CoalQualityMapper;
import com.coalblend.mapper.FinalProductInspectionMapper;
import com.coalblend.mapper.ProductBatchMapper;
import com.coalblend.service.chain.BatchNoGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/finalInspection")
@RequiredArgsConstructor
public class FinalProductInspectionController {

    private final FinalProductInspectionMapper inspectionMapper;
    private final ProductBatchMapper productBatchMapper;
    private final CoalQualityMapper coalQualityMapper;
    private final BlendPlanMapper blendPlanMapper;
    private final BatchNoGenerator batchNoGenerator;

    @GetMapping("/page")
    public Result<IPage<FinalProductInspection>> page(@RequestParam(defaultValue = "1") long current,
                                                      @RequestParam(defaultValue = "10") long size,
                                                      @RequestParam(required = false) Long orderId,
                                                      @RequestParam(required = false) Long planId) {
        LambdaQueryWrapper<FinalProductInspection> w = new LambdaQueryWrapper<>();
        if (orderId != null) w.eq(FinalProductInspection::getOrderId, orderId);
        if (planId != null) w.eq(FinalProductInspection::getPlanId, planId);
        w.orderByDesc(FinalProductInspection::getId);
        return Result.ok(inspectionMapper.selectPage(new Page<>(current, size), w));
    }

    @PostMapping("/add")
    @Transactional(rollbackFor = Exception.class)
    public Result<FinalProductInspection> add(@RequestBody FinalProductInspection body) {
        ProductBatch product = productBatchMapper.selectById(body.getProductBatchId());
        if (product == null) throw new BusinessException("产品批次不存在");
        if (!StringUtils.hasText(body.getReportNo())) body.setReportNo(batchNoGenerator.reportNo());
        if (body.getSampleTime() == null) body.setSampleTime(LocalDateTime.now());
        if (body.getOrderId() == null) body.setOrderId(product.getOrderId());
        if (body.getPlanId() == null) body.setPlanId(product.getPlanId());
        inspectionMapper.insert(body);

        CoalQuality q = new CoalQuality();
        q.setBatchNo(product.getProductBatchNo());
        q.setSampleStage("final_product");
        q.setRelatedBatchNo(product.getProductBatchNo());
        q.setSamplePoint(body.getSamplePoint());
        q.setSampleTime(body.getSampleTime());
        q.setAshContent(body.getAshContent());
        q.setSulfurContent(body.getSulfurContent());
        q.setMoistureContent(body.getMoistureContent());
        q.setVolatileContent(body.getVolatileContent());
        q.setCalorificValue(body.getCalorificValue());
        q.setReportNo(body.getReportNo());
        q.setStandardBasis(body.getStandardBasis());
        q.setStatus(1);
        coalQualityMapper.insert(q);
        if (body.getPlanId() != null) {
            blendPlanMapper.update(null, new LambdaUpdateWrapper<BlendPlan>()
                    .eq(BlendPlan::getId, body.getPlanId())
                    .set(BlendPlan::getTraceStatus, "inspected"));
        }
        return Result.ok(body);
    }

    @GetMapping("/byProduct/{productBatchId}")
    public Result<List<FinalProductInspection>> byProduct(@PathVariable Long productBatchId) {
        return Result.ok(inspectionMapper.selectList(new LambdaQueryWrapper<FinalProductInspection>()
                .eq(FinalProductInspection::getProductBatchId, productBatchId)
                .orderByDesc(FinalProductInspection::getId)));
    }

    @GetMapping("/byOrder/{orderId}")
    public Result<List<FinalProductInspection>> byOrder(@PathVariable Long orderId) {
        return Result.ok(inspectionMapper.selectList(new LambdaQueryWrapper<FinalProductInspection>()
                .eq(FinalProductInspection::getOrderId, orderId)
                .orderByDesc(FinalProductInspection::getId)));
    }
}
