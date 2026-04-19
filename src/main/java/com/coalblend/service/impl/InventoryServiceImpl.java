package com.coalblend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coalblend.common.exception.BusinessException;
import com.coalblend.entity.Inventory;
import com.coalblend.mapper.CoalTypeMapper;
import com.coalblend.mapper.InventoryMapper;
import com.coalblend.service.InventoryService;
import com.coalblend.vo.InventoryAvailableVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryMapper inventoryMapper;
    private final CoalTypeMapper coalTypeMapper;

    @Override
    public IPage<Inventory> page(long current, long size, Long coalId, Integer status, String warehouseCode) {
        Page<Inventory> page = new Page<>(current, size);
        LambdaQueryWrapper<Inventory> w = new LambdaQueryWrapper<>();
        if (coalId != null) {
            w.eq(Inventory::getCoalId, coalId);
        }
        if (status != null) {
            w.eq(Inventory::getStatus, status);
        }
        if (StringUtils.hasText(warehouseCode)) {
            w.like(Inventory::getWarehouseCode, warehouseCode);
        }
        w.orderByDesc(Inventory::getId);
        return inventoryMapper.selectPage(page, w);
    }

    @Override
    public List<Inventory> listByCoal(Long coalId) {
        return inventoryMapper.selectList(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getCoalId, coalId)
                .orderByAsc(Inventory::getWarehouseCode));
    }

    @Override
    public InventoryAvailableVO availableSummary(Long coalId) {
        List<Inventory> rows = inventoryMapper.selectList(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getCoalId, coalId)
                .eq(Inventory::getStatus, 1));
        BigDecimal total = rows.stream()
                .map(Inventory::getAvailableQuantity)
                .filter(q -> q != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<InventoryAvailableVO.WarehouseLine> lines = rows.stream()
                .map(r -> new InventoryAvailableVO.WarehouseLine(r.getWarehouseCode(), r.getAvailableQuantity()))
                .collect(Collectors.toList());
        return new InventoryAvailableVO(coalId, total, lines);
    }

    @Override
    public Inventory getById(Long id) {
        Inventory row = inventoryMapper.selectById(id);
        if (row == null) {
            throw new BusinessException(404, "库存记录不存在");
        }
        return row;
    }

    @Override
    public void add(Inventory entity) {
        if (entity.getCoalId() == null) {
            throw new BusinessException("煤种不能为空");
        }
        if (coalTypeMapper.selectById(entity.getCoalId()) == null) {
            throw new BusinessException(404, "煤种不存在");
        }
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        inventoryMapper.insert(entity);
    }

    @Override
    public void update(Inventory entity) {
        if (entity.getId() == null) {
            throw new BusinessException("id 不能为空");
        }
        getById(entity.getId());
        inventoryMapper.updateById(entity);
    }

    @Override
    public void delete(Long id) {
        getById(id);
        inventoryMapper.deleteById(id);
    }
}
