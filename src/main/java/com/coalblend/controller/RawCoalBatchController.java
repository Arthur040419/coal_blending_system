package com.coalblend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import java.time.LocalDateTime;

@RestController
@RequestMapping("/rawCoalBatch")
@RequiredArgsConstructor
public class RawCoalBatchController {

    private final RawCoalBatchMapper rawCoalBatchMapper;
    private final MineSourceMapper mineSourceMapper;
    private final CoalQualityMapper coalQualityMapper;
    private final InventoryMapper inventoryMapper;
    private final BatchNoGenerator batchNoGenerator;
    private final BatchLineageService lineageService;

    @GetMapping("/page")
    public Result<IPage<RawCoalBatch>> page(@RequestParam(defaultValue = "1") long current,
                                            @RequestParam(defaultValue = "10") long size,
                                            @RequestParam(required = false) Long sourceId,
                                            @RequestParam(required = false) Long coalId,
                                            @RequestParam(required = false) String status,
                                            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<RawCoalBatch> w = new LambdaQueryWrapper<>();
        if (sourceId != null) w.eq(RawCoalBatch::getSourceId, sourceId);
        if (coalId != null) w.eq(RawCoalBatch::getCoalId, coalId);
        if (StringUtils.hasText(status)) w.eq(RawCoalBatch::getStatus, status);
        if (StringUtils.hasText(keyword)) w.like(RawCoalBatch::getRawBatchNo, keyword);
        w.orderByDesc(RawCoalBatch::getId);
        return Result.ok(rawCoalBatchMapper.selectPage(new Page<>(current, size), w));
    }

    @GetMapping("/detail/{id}")
    public Result<RawCoalBatch> detail(@PathVariable Long id) {
        RawCoalBatch row = rawCoalBatchMapper.selectById(id);
        if (row == null) throw new BusinessException(404, "原煤批次不存在");
        return Result.ok(row);
    }

    @GetMapping("/bySource/{sourceId}")
    public Result<java.util.List<RawCoalBatch>> bySource(@PathVariable Long sourceId) {
        return Result.ok(rawCoalBatchMapper.selectList(new LambdaQueryWrapper<RawCoalBatch>()
                .eq(RawCoalBatch::getSourceId, sourceId)
                .orderByDesc(RawCoalBatch::getId)));
    }

    @PostMapping("/add")
    @Transactional(rollbackFor = Exception.class)
    public Result<RawCoalBatch> add(@RequestBody RawCoalBatch body) {
        prepareAndInsertRaw(body);
        return Result.ok(body);
    }

    @PostMapping("/addWithQuality")
    @Transactional(rollbackFor = Exception.class)
    public Result<RawCoalBatch> addWithQuality(@RequestBody RawCoalWithQualityDTO dto) {
        RawCoalBatch raw = dto.toRaw();
        prepareAndInsertRaw(raw);
        if (dto.getQuality() != null) {
            CoalQuality q = dto.getQuality();
            q.setCoalId(raw.getCoalId());
            q.setBatchNo(raw.getRawBatchNo());
            q.setSampleStage("raw_coal");
            q.setRelatedBatchNo(raw.getRawBatchNo());
            if (!StringUtils.hasText(q.getReportNo())) q.setReportNo(batchNoGenerator.reportNo());
            if (q.getSampleTime() == null) q.setSampleTime(LocalDateTime.now());
            if (q.getStatus() == null) q.setStatus(1);
            coalQualityMapper.insert(q);
        }
        storeRaw(raw.getId());
        return Result.ok(raw);
    }

    @PostMapping("/store/{id}")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> store(@PathVariable Long id) {
        storeRaw(id);
        return Result.ok();
    }

    @PutMapping("/update")
    public Result<Void> update(@RequestBody RawCoalBatch body) {
        if (body.getId() == null) throw new BusinessException("id不能为空");
        rawCoalBatchMapper.updateById(body);
        return Result.ok();
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        rawCoalBatchMapper.deleteById(id);
        return Result.ok();
    }

    private void prepareAndInsertRaw(RawCoalBatch body) {
        if (body.getSourceId() == null) throw new BusinessException("矿区来源不能为空");
        if (body.getOutputQuantity() == null || body.getOutputQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("原煤产量必须大于0");
        }
        MineSource source = mineSourceMapper.selectById(body.getSourceId());
        if (source == null) throw new BusinessException("矿区来源不存在");
        if (!StringUtils.hasText(body.getRawBatchNo())) body.setRawBatchNo(batchNoGenerator.rawBatchNo());
        if (!StringUtils.hasText(body.getStatus())) body.setStatus("produced");
        rawCoalBatchMapper.insert(body);
        lineageService.record(source.getSourceCode(), "mine_source", body.getRawBatchNo(), "raw_coal",
                "production", body.getOutputQuantity(), null, null, "矿区来源生产原煤批次");
    }

    private void storeRaw(Long rawId) {
        RawCoalBatch raw = rawCoalBatchMapper.selectById(rawId);
        if (raw == null) throw new BusinessException("原煤批次不存在");
        Inventory inv = new Inventory();
        inv.setCoalId(raw.getCoalId());
        inv.setWarehouseCode(StringUtils.hasText(raw.getWarehouseCode()) ? raw.getWarehouseCode() : raw.getDestination());
        inv.setStockQuantity(raw.getOutputQuantity());
        inv.setAvailableQuantity(raw.getOutputQuantity());
        inv.setMaterialStage("raw_coal");
        inv.setRawBatchNo(raw.getRawBatchNo());
        inv.setLockedQuantity(BigDecimal.ZERO);
        inv.setStatus(1);
        inv.setRemark("原煤批次入库：" + raw.getRawBatchNo());
        inventoryMapper.insert(inv);
        RawCoalBatch patch = new RawCoalBatch();
        patch.setId(raw.getId());
        patch.setStatus("stored");
        rawCoalBatchMapper.updateById(patch);
    }

    @Data
    public static class RawCoalWithQualityDTO {
        private Long sourceId;
        private Long coalId;
        private java.time.LocalDate productionDate;
        private String shiftNo;
        private BigDecimal outputQuantity;
        private BigDecimal gangueRate;
        private String destination;
        private String warehouseCode;
        private String remark;
        private CoalQuality quality;

        RawCoalBatch toRaw() {
            RawCoalBatch r = new RawCoalBatch();
            r.setSourceId(sourceId);
            r.setCoalId(coalId);
            r.setProductionDate(productionDate);
            r.setShiftNo(shiftNo);
            r.setOutputQuantity(outputQuantity);
            r.setGangueRate(gangueRate);
            r.setDestination(destination);
            r.setWarehouseCode(warehouseCode);
            r.setRemark(remark);
            return r;
        }
    }
}
