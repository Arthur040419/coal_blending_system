package com.coalblend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coalblend.common.exception.BusinessException;
import com.coalblend.entity.BlendPlanDetail;
import com.coalblend.entity.CoalQuality;
import com.coalblend.entity.CoalType;
import com.coalblend.entity.Inventory;
import com.coalblend.mapper.BlendPlanDetailMapper;
import com.coalblend.mapper.CoalQualityMapper;
import com.coalblend.mapper.CoalTypeMapper;
import com.coalblend.mapper.InventoryMapper;
import com.coalblend.service.CoalTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CoalTypeServiceImpl implements CoalTypeService {

    private final CoalTypeMapper coalTypeMapper;
    private final CoalQualityMapper coalQualityMapper;
    private final InventoryMapper inventoryMapper;
    private final BlendPlanDetailMapper blendPlanDetailMapper;

    @Override
    public IPage<CoalType> page(long current, long size, String keyword, String coalCategory, Integer blendableFlag) {
        Page<CoalType> page = new Page<>(current, size);
        LambdaQueryWrapper<CoalType> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(CoalType::getCoalName, keyword).or().like(CoalType::getCoalCode, keyword));
        }
        if (StringUtils.hasText(coalCategory)) {
            wrapper.like(CoalType::getCoalCategory, coalCategory);
        }
        if (blendableFlag != null) {
            wrapper.eq(CoalType::getBlendableFlag, blendableFlag);
        }
        wrapper.orderByDesc(CoalType::getId);
        return coalTypeMapper.selectPage(page, wrapper);
    }

    @Override
    public CoalType getById(Long id) {
        CoalType row = coalTypeMapper.selectById(id);
        if (row == null) {
            throw new BusinessException(404, "煤种不存在");
        }
        return row;
    }

    @Override
    public void add(CoalType entity) {
        if (!StringUtils.hasText(entity.getCoalCode())) {
            throw new BusinessException("煤种编号不能为空");
        }
        if (!StringUtils.hasText(entity.getCoalName())) {
            throw new BusinessException("煤种名称不能为空");
        }
        if (entity.getBlendableFlag() == null) {
            entity.setBlendableFlag(1);
        }
        Long cnt = coalTypeMapper.selectCount(
                new LambdaQueryWrapper<CoalType>().eq(CoalType::getCoalCode, entity.getCoalCode()));
        if (cnt != null && cnt > 0) {
            throw new BusinessException("煤种编号已存在");
        }
        coalTypeMapper.insert(entity);
    }

    @Override
    public void update(CoalType entity) {
        if (entity.getId() == null) {
            throw new BusinessException("id 不能为空");
        }
        getById(entity.getId());
        if (StringUtils.hasText(entity.getCoalCode())) {
            Long cnt = coalTypeMapper.selectCount(new LambdaQueryWrapper<CoalType>()
                    .eq(CoalType::getCoalCode, entity.getCoalCode())
                    .ne(CoalType::getId, entity.getId()));
            if (cnt != null && cnt > 0) {
                throw new BusinessException("煤种编号已存在");
            }
        }
        coalTypeMapper.updateById(entity);
    }

    @Override
    public void delete(Long id) {
        getById(id);
        Long q = coalQualityMapper.selectCount(new LambdaQueryWrapper<CoalQuality>().eq(CoalQuality::getCoalId, id));
        if (q != null && q > 0) {
            throw new BusinessException("该煤种存在煤质记录，无法删除");
        }
        Long inv = inventoryMapper.selectCount(new LambdaQueryWrapper<Inventory>().eq(Inventory::getCoalId, id));
        if (inv != null && inv > 0) {
            throw new BusinessException("该煤种存在库存记录，无法删除");
        }
        Long d = blendPlanDetailMapper.selectCount(new LambdaQueryWrapper<BlendPlanDetail>().eq(BlendPlanDetail::getCoalId, id));
        if (d != null && d > 0) {
            throw new BusinessException("该煤种已被配煤方案引用，无法删除");
        }
        coalTypeMapper.deleteById(id);
    }
}
