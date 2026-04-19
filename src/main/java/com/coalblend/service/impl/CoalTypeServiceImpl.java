package com.coalblend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coalblend.common.exception.BusinessException;
import com.coalblend.entity.CoalType;
import com.coalblend.mapper.CoalTypeMapper;
import com.coalblend.service.CoalTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CoalTypeServiceImpl implements CoalTypeService {

    private final CoalTypeMapper coalTypeMapper;

    @Override
    public IPage<CoalType> page(long current, long size, String keyword) {
        Page<CoalType> page = new Page<>(current, size);
        LambdaQueryWrapper<CoalType> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(CoalType::getCoalName, keyword).or().like(CoalType::getCoalCode, keyword));
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
        coalTypeMapper.deleteById(id);
    }
}
