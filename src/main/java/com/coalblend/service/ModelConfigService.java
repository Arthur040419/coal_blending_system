package com.coalblend.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.coalblend.entity.ModelConfig;

import java.util.List;

public interface ModelConfigService {

    List<ModelConfig> listAll();

    IPage<ModelConfig> page(long current, long size);

    ModelConfig getById(Long id);

    void add(ModelConfig entity);

    void update(ModelConfig entity);

    void updateStatus(Long id, Integer status);
}
