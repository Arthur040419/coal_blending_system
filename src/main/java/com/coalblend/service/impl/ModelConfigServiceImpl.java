package com.coalblend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coalblend.common.exception.BusinessException;
import com.coalblend.entity.ModelConfig;
import com.coalblend.mapper.ModelConfigMapper;
import com.coalblend.service.ModelConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ModelConfigServiceImpl implements ModelConfigService {

    private final ModelConfigMapper modelConfigMapper;

    @Override
    public List<ModelConfig> listAll() {
        return modelConfigMapper.selectList(new LambdaQueryWrapper<ModelConfig>().orderByAsc(ModelConfig::getId));
    }

    @Override
    public IPage<ModelConfig> page(long current, long size, String keyword, String modelType) {
        LambdaQueryWrapper<ModelConfig> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(modelType)) {
            w.eq(ModelConfig::getModelType, modelType);
        }
        if (StringUtils.hasText(keyword)) {
            w.and(q -> q.like(ModelConfig::getModelName, keyword).or().like(ModelConfig::getRemark, keyword));
        }
        w.orderByDesc(ModelConfig::getId);
        return modelConfigMapper.selectPage(new Page<>(current, size), w);
    }

    @Override
    public ModelConfig getById(Long id) {
        ModelConfig row = modelConfigMapper.selectById(id);
        if (row == null) {
            throw new BusinessException(404, "配置不存在");
        }
        return row;
    }

    @Override
    public void add(ModelConfig entity) {
        if (!StringUtils.hasText(entity.getModelName())) {
            throw new BusinessException("模型名称不能为空");
        }
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        modelConfigMapper.insert(entity);
    }

    @Override
    public void update(ModelConfig entity) {
        if (entity.getId() == null) {
            throw new BusinessException("id 不能为空");
        }
        getById(entity.getId());
        modelConfigMapper.updateById(entity);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        getById(id);
        ModelConfig u = new ModelConfig();
        u.setId(id);
        u.setStatus(status);
        modelConfigMapper.updateById(u);
    }

    @Override
    public void delete(Long id) {
        getById(id);
        modelConfigMapper.deleteById(id);
    }
}
