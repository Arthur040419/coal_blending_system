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
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/washProcess")
@RequiredArgsConstructor
public class WashProcessController {

    private final WashProcessBatchMapper washMapper;
    private final WashInputDetailMapper inputMapper;
    private final RawCoalBatchMapper rawMapper;
    private final ProductBatchMapper productMapper;
    private final InventoryMapper inventoryMapper;
    private final CoalQualityMapper coalQualityMapper;
    private final BatchNoGenerator batchNoGenerator;
    private final BatchLineageService lineageService;

    @GetMapping("/page")
    public Result<IPage<WashProcessBatch>> page(@RequestParam(defaultValue = "1") long current,
                                                @RequestParam(defaultValue = "10") long size,
                                                @RequestParam(required = false) String status,
                                                @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<WashProcessBatch> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) w.eq(WashProcessBatch::getStatus, status);
        if (StringUtils.hasText(keyword)) w.like(WashProcessBatch::getWashBatchNo, keyword);
        w.orderByDesc(WashProcessBatch::getId);
        return Result.ok(washMapper.selectPage(new Page<>(current, size), w));
    }

    @GetMapping("/detail/{id}")
    public Result<WashProcessBatch> detail(@PathVariable Long id) {
        WashProcessBatch row = washMapper.selectById(id);
        if (row == null) throw new BusinessException(404, "洗选批次不存在");
        return Result.ok(row);
    }

    @PostMapping("/create")
    public Result<WashProcessBatch> create(@RequestBody WashProcessBatch body) {
        if (!StringUtils.hasText(body.getWashBatchNo())) body.setWashBatchNo(batchNoGenerator.washBatchNo());
        if (!StringUtils.hasText(body.getStatus())) body.setStatus("created");
        washMapper.insert(body);
        return Result.ok(body);
    }

    @PostMapping("/addInput")
    @Transactional(rollbackFor = Exception.class)
    public Result<WashInputDetail> addInput(@RequestBody WashInputDetail body) {
        WashProcessBatch wash = washMapper.selectById(body.getWashBatchId());
        RawCoalBatch raw = rawMapper.selectById(body.getRawBatchId());
        if (wash == null) throw new BusinessException("洗选批次不存在");
        if (raw == null) throw new BusinessException("原煤批次不存在");
        if (body.getInputQuantity() == null || body.getInputQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("投入量必须大于0");
        }
        inputMapper.insert(body);
        BigDecimal feed = wash.getFeedQuantity() == null ? BigDecimal.ZERO : wash.getFeedQuantity();
        WashProcessBatch patch = new WashProcessBatch();
        patch.setId(wash.getId());
        patch.setFeedQuantity(feed.add(body.getInputQuantity()));
        washMapper.updateById(patch);
        lineageService.record(raw.getRawBatchNo(), "raw_coal", wash.getWashBatchNo(), "wash_batch",
                "washing", body.getInputQuantity(), body.getInputRatio(), null, "原煤批次投入洗选");
        RawCoalBatch rawPatch = new RawCoalBatch();
        rawPatch.setId(raw.getId());
        rawPatch.setStatus("washing");
        rawMapper.updateById(rawPatch);
        return Result.ok(body);
    }

    @PostMapping("/start/{id}")
    public Result<Void> start(@PathVariable Long id) {
        WashProcessBatch patch = new WashProcessBatch();
        patch.setId(id);
        patch.setStatus("running");
        if (washMapper.selectById(id) == null) throw new BusinessException("洗选批次不存在");
        washMapper.updateById(patch);
        return Result.ok();
    }

    @PostMapping("/finish")
    @Transactional(rollbackFor = Exception.class)
    public Result<List<ProductBatch>> finish(@RequestBody FinishDTO dto) {
        WashProcessBatch wash = washMapper.selectById(dto.getWashBatchId());
        if (wash == null) throw new BusinessException("洗选批次不存在");
        WashProcessBatch patch = new WashProcessBatch();
        patch.setId(wash.getId());
        patch.setEndTime(dto.getEndTime() == null ? LocalDateTime.now() : dto.getEndTime());
        patch.setCleanCoalYield(dto.getCleanCoalYield());
        patch.setMiddlingsYield(dto.getMiddlingsYield());
        patch.setSlimeYield(dto.getSlimeYield());
        patch.setGangueYield(dto.getGangueYield());
        patch.setMediumDensity(dto.getMediumDensity());
        patch.setSeparationDensity(dto.getSeparationDensity());
        patch.setStatus("finished");
        washMapper.updateById(patch);

        List<ProductBatch> out = new ArrayList<>();
        Long sourceCoalId = firstInputCoalId(wash.getId());
        if (dto.getProducts() != null) {
            for (ProductBatch p : dto.getProducts()) {
                if (!StringUtils.hasText(p.getProductBatchNo())) {
                    p.setProductBatchNo(batchNoGenerator.productBatchNo(p.getProductType()));
                }
                p.setWashBatchId(wash.getId());
                if (p.getCoalId() == null) p.setCoalId(sourceCoalId);
                if (p.getAvailableQuantity() == null) p.setAvailableQuantity(p.getQuantity());
                if (!StringUtils.hasText(p.getStatus())) p.setStatus("available");
                productMapper.insert(p);
                insertProductInventory(p);
                insertProductQuality(p);
                lineageService.record(wash.getWashBatchNo(), "wash_batch", p.getProductBatchNo(), "product_batch",
                        "product_output", p.getQuantity(), null, wash.getOperatorName(), "洗选批次产出产品批次");
                out.add(p);
            }
        }
        List<WashInputDetail> inputs = inputMapper.selectList(new LambdaQueryWrapper<WashInputDetail>()
                .eq(WashInputDetail::getWashBatchId, wash.getId()));
        for (WashInputDetail in : inputs) {
            RawCoalBatch rawPatch = new RawCoalBatch();
            rawPatch.setId(in.getRawBatchId());
            rawPatch.setStatus("washed");
            rawMapper.updateById(rawPatch);
            deductRawInventory(in);
        }
        return Result.ok(out);
    }

    private Long firstInputCoalId(Long washBatchId) {
        WashInputDetail first = inputMapper.selectOne(new LambdaQueryWrapper<WashInputDetail>()
                .eq(WashInputDetail::getWashBatchId, washBatchId)
                .orderByAsc(WashInputDetail::getId)
                .last("LIMIT 1"));
        if (first == null) return null;
        RawCoalBatch raw = rawMapper.selectById(first.getRawBatchId());
        return raw == null ? null : raw.getCoalId();
    }

    @GetMapping("/inputs/{washBatchId}")
    public Result<List<WashInputDetail>> inputs(@PathVariable Long washBatchId) {
        return Result.ok(inputMapper.selectList(new LambdaQueryWrapper<WashInputDetail>()
                .eq(WashInputDetail::getWashBatchId, washBatchId)
                .orderByAsc(WashInputDetail::getId)));
    }

    @GetMapping("/products/{washBatchId}")
    public Result<List<ProductBatch>> products(@PathVariable Long washBatchId) {
        return Result.ok(productMapper.selectList(new LambdaQueryWrapper<ProductBatch>()
                .eq(ProductBatch::getWashBatchId, washBatchId)
                .orderByAsc(ProductBatch::getId)));
    }

    private void insertProductInventory(ProductBatch p) {
        Inventory inv = new Inventory();
        inv.setCoalId(null);
        inv.setWarehouseCode(p.getWarehouseCode());
        inv.setStockQuantity(p.getQuantity());
        inv.setAvailableQuantity(p.getAvailableQuantity());
        inv.setMaterialStage("product_batch");
        inv.setProductBatchNo(p.getProductBatchNo());
        inv.setLockedQuantity(BigDecimal.ZERO);
        inv.setStatus(1);
        inv.setRemark("产品批次入库：" + p.getProductBatchNo());
        inventoryMapper.insert(inv);
    }

    private void insertProductQuality(ProductBatch p) {
        CoalQuality q = new CoalQuality();
        q.setSampleStage(p.getProductType());
        q.setRelatedBatchNo(p.getProductBatchNo());
        q.setSampleTime(LocalDateTime.now());
        q.setAshContent(p.getAshContent());
        q.setSulfurContent(p.getSulfurContent());
        q.setMoistureContent(p.getMoistureContent());
        q.setVolatileContent(p.getVolatileContent());
        q.setCalorificValue(p.getCalorificValue());
        q.setReportNo(batchNoGenerator.reportNo());
        q.setStatus(1);
        coalQualityMapper.insert(q);
    }

    private void deductRawInventory(WashInputDetail in) {
        RawCoalBatch raw = rawMapper.selectById(in.getRawBatchId());
        if (raw == null || !StringUtils.hasText(raw.getRawBatchNo())) return;
        Inventory inv = inventoryMapper.selectOne(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getRawBatchNo, raw.getRawBatchNo())
                .orderByAsc(Inventory::getId)
                .last("LIMIT 1"));
        if (inv == null || inv.getAvailableQuantity() == null) return;
        BigDecimal next = inv.getAvailableQuantity().subtract(in.getInputQuantity()).max(BigDecimal.ZERO);
        inventoryMapper.update(null, new LambdaUpdateWrapper<Inventory>()
                .eq(Inventory::getId, inv.getId())
                .set(Inventory::getAvailableQuantity, next));
    }

    @Data
    public static class FinishDTO {
        private Long washBatchId;
        private LocalDateTime endTime;
        private BigDecimal cleanCoalYield;
        private BigDecimal middlingsYield;
        private BigDecimal slimeYield;
        private BigDecimal gangueYield;
        private BigDecimal mediumDensity;
        private BigDecimal separationDensity;
        private List<ProductBatch> products;
    }
}
