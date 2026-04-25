package com.coalblend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coalblend.common.exception.BusinessException;
import com.coalblend.common.result.Result;
import com.coalblend.entity.ProductBatch;
import com.coalblend.mapper.ProductBatchMapper;
import com.coalblend.service.chain.BatchNoGenerator;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/productBatch")
@RequiredArgsConstructor
public class ProductBatchController {

    private final ProductBatchMapper productBatchMapper;
    private final BatchNoGenerator batchNoGenerator;

    @GetMapping("/page")
    public Result<IPage<ProductBatch>> page(@RequestParam(defaultValue = "1") long current,
                                            @RequestParam(defaultValue = "10") long size,
                                            @RequestParam(required = false) String productType,
                                            @RequestParam(required = false) String status,
                                            @RequestParam(required = false) String keyword) {
        return Result.ok(productBatchMapper.selectPage(new Page<>(current, size),
                buildWrapper(productType, status, keyword, null, null)));
    }

    @GetMapping("/available")
    public Result<IPage<ProductBatch>> available(@RequestParam(defaultValue = "1") long current,
                                                 @RequestParam(defaultValue = "10") long size,
                                                 @RequestParam(required = false) String productType,
                                                 @RequestParam(required = false) BigDecimal minCalorific,
                                                 @RequestParam(required = false) BigDecimal maxSulfur) {
        LambdaQueryWrapper<ProductBatch> w = buildWrapper(productType, "available", null, minCalorific, maxSulfur);
        w.gt(ProductBatch::getAvailableQuantity, BigDecimal.ZERO);
        return Result.ok(productBatchMapper.selectPage(new Page<>(current, size), w));
    }

    @GetMapping("/detail/{id}")
    public Result<ProductBatch> detail(@PathVariable Long id) {
        ProductBatch row = productBatchMapper.selectById(id);
        if (row == null) throw new BusinessException(404, "产品批次不存在");
        return Result.ok(row);
    }

    @GetMapping("/byWash/{washBatchId}")
    public Result<List<ProductBatch>> byWash(@PathVariable Long washBatchId) {
        return Result.ok(productBatchMapper.selectList(new LambdaQueryWrapper<ProductBatch>()
                .eq(ProductBatch::getWashBatchId, washBatchId)
                .orderByDesc(ProductBatch::getId)));
    }

    @GetMapping("/byOrder/{orderId}")
    public Result<List<ProductBatch>> byOrder(@PathVariable Long orderId) {
        return Result.ok(productBatchMapper.selectList(new LambdaQueryWrapper<ProductBatch>()
                .eq(ProductBatch::getOrderId, orderId)
                .orderByDesc(ProductBatch::getId)));
    }

    @PostMapping("/add")
    public Result<ProductBatch> add(@RequestBody ProductBatch body) {
        if (!StringUtils.hasText(body.getProductType())) throw new BusinessException("产品类型不能为空");
        if (body.getQuantity() == null) throw new BusinessException("产量不能为空");
        if (!StringUtils.hasText(body.getProductBatchNo())) {
            body.setProductBatchNo(batchNoGenerator.productBatchNo(body.getProductType()));
        }
        if (body.getAvailableQuantity() == null) body.setAvailableQuantity(body.getQuantity());
        if (!StringUtils.hasText(body.getStatus())) body.setStatus("available");
        productBatchMapper.insert(body);
        return Result.ok(body);
    }

    @PutMapping("/update")
    public Result<Void> update(@RequestBody ProductBatch body) {
        if (body.getId() == null) throw new BusinessException("id不能为空");
        productBatchMapper.updateById(body);
        return Result.ok();
    }

    @PutMapping("/status")
    public Result<Void> status(@RequestBody StatusDTO dto) {
        ProductBatch patch = new ProductBatch();
        patch.setId(dto.getId());
        patch.setStatus(dto.getStatus());
        productBatchMapper.updateById(patch);
        return Result.ok();
    }

    private LambdaQueryWrapper<ProductBatch> buildWrapper(String productType, String status, String keyword,
                                                         BigDecimal minCalorific, BigDecimal maxSulfur) {
        LambdaQueryWrapper<ProductBatch> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(productType)) w.eq(ProductBatch::getProductType, productType);
        if (StringUtils.hasText(status)) w.eq(ProductBatch::getStatus, status);
        if (StringUtils.hasText(keyword)) {
            w.and(q -> q.like(ProductBatch::getProductBatchNo, keyword)
                    .or().like(ProductBatch::getProductName, keyword));
        }
        if (minCalorific != null) w.ge(ProductBatch::getCalorificValue, minCalorific);
        if (maxSulfur != null) w.le(ProductBatch::getSulfurContent, maxSulfur);
        w.orderByDesc(ProductBatch::getId);
        return w;
    }

    @Data
    public static class StatusDTO {
        private Long id;
        private String status;
    }
}
