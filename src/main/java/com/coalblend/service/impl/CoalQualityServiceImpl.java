package com.coalblend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coalblend.common.exception.BusinessException;
import com.coalblend.entity.CoalQuality;
import com.coalblend.mapper.CoalQualityMapper;
import com.coalblend.mapper.CoalTypeMapper;
import com.coalblend.service.CoalQualityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CoalQualityServiceImpl implements CoalQualityService {

    private final CoalQualityMapper coalQualityMapper;
    private final CoalTypeMapper coalTypeMapper;

    @Override
    public IPage<CoalQuality> page(long current, long size, Long coalId, Integer status, String keyword) {
        Page<CoalQuality> page = new Page<>(current, size);
        LambdaQueryWrapper<CoalQuality> w = new LambdaQueryWrapper<>();
        if (coalId != null) {
            w.eq(CoalQuality::getCoalId, coalId);
        }
        if (status != null) {
            w.eq(CoalQuality::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            w.like(CoalQuality::getBatchNo, keyword);
        }
        w.orderByDesc(CoalQuality::getSampleTime);
        return coalQualityMapper.selectPage(page, w);
    }

    @Override
    public List<CoalQuality> listByCoal(Long coalId) {
        return coalQualityMapper.selectList(new LambdaQueryWrapper<CoalQuality>()
                .eq(CoalQuality::getCoalId, coalId)
                .orderByDesc(CoalQuality::getSampleTime));
    }

    @Override
    public CoalQuality latest(Long coalId) {
        CoalQuality q = coalQualityMapper.selectOne(new LambdaQueryWrapper<CoalQuality>()
                .eq(CoalQuality::getCoalId, coalId)
                .eq(CoalQuality::getStatus, 1)
                .orderByDesc(CoalQuality::getSampleTime)
                .last("limit 1"));
        if (q == null) {
            throw new BusinessException(404, "暂无有效煤质数据");
        }
        return q;
    }

    @Override
    public CoalQuality getById(Long id) {
        CoalQuality row = coalQualityMapper.selectById(id);
        if (row == null) {
            throw new BusinessException(404, "煤质记录不存在");
        }
        return row;
    }

    @Override
    public void add(CoalQuality entity) {
        if (entity.getCoalId() == null) {
            throw new BusinessException("煤种不能为空");
        }
        if (coalTypeMapper.selectById(entity.getCoalId()) == null) {
            throw new BusinessException(404, "煤种不存在");
        }
        if (!StringUtils.hasText(entity.getBatchNo())) {
            throw new BusinessException("批次号不能为空");
        }
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        coalQualityMapper.insert(entity);
    }

    @Override
    public void update(CoalQuality entity) {
        if (entity.getId() == null) {
            throw new BusinessException("id 不能为空");
        }
        getById(entity.getId());
        coalQualityMapper.updateById(entity);
    }

    @Override
    public void delete(Long id) {
        getById(id);
        coalQualityMapper.deleteById(id);
    }
}
