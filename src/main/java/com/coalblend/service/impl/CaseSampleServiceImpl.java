package com.coalblend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coalblend.common.exception.BusinessException;
import com.coalblend.entity.CaseSample;
import com.coalblend.mapper.CaseSampleMapper;
import com.coalblend.service.CaseSampleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CaseSampleServiceImpl implements CaseSampleService {

    private final CaseSampleMapper caseSampleMapper;

    @Override
    public IPage<CaseSample> page(long current, long size, Integer status, String keyword) {
        Page<CaseSample> page = new Page<>(current, size);
        LambdaQueryWrapper<CaseSample> w = new LambdaQueryWrapper<>();
        if (status != null) {
            w.eq(CaseSample::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            w.and(q -> q.like(CaseSample::getCaseName, keyword)
                    .or().like(CaseSample::getCaseCode, keyword)
                    .or().like(CaseSample::getOrderDesc, keyword)
                    .or().like(CaseSample::getBlendDesc, keyword)
                    .or().like(CaseSample::getResultDesc, keyword));
        }
        w.orderByDesc(CaseSample::getId);
        return caseSampleMapper.selectPage(page, w);
    }

    @Override
    public List<CaseSample> search(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        return caseSampleMapper.selectList(new LambdaQueryWrapper<CaseSample>()
                .eq(CaseSample::getStatus, 1)
                .and(w -> w.like(CaseSample::getCaseName, keyword)
                        .or().like(CaseSample::getOrderDesc, keyword)
                        .or().like(CaseSample::getBlendDesc, keyword)
                        .or().like(CaseSample::getResultDesc, keyword))
                .orderByDesc(CaseSample::getId)
                .last("limit 50"));
    }

    @Override
    public CaseSample getById(Long id) {
        CaseSample row = caseSampleMapper.selectById(id);
        if (row == null) {
            throw new BusinessException(404, "案例不存在");
        }
        return row;
    }

    @Override
    public void add(CaseSample entity) {
        Long cnt = caseSampleMapper.selectCount(
                new LambdaQueryWrapper<CaseSample>().eq(CaseSample::getCaseCode, entity.getCaseCode()));
        if (cnt != null && cnt > 0) {
            throw new BusinessException("案例编号已存在");
        }
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        caseSampleMapper.insert(entity);
    }

    @Override
    public void update(CaseSample entity) {
        if (entity.getId() == null) {
            throw new BusinessException("id 不能为空");
        }
        getById(entity.getId());
        if (StringUtils.hasText(entity.getCaseCode())) {
            Long cnt = caseSampleMapper.selectCount(new LambdaQueryWrapper<CaseSample>()
                    .eq(CaseSample::getCaseCode, entity.getCaseCode())
                    .ne(CaseSample::getId, entity.getId()));
            if (cnt != null && cnt > 0) {
                throw new BusinessException("案例编号已存在");
            }
        }
        caseSampleMapper.updateById(entity);
    }

    @Override
    public void delete(Long id) {
        getById(id);
        caseSampleMapper.deleteById(id);
    }
}
