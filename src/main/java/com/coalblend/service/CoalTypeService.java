package com.coalblend.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.coalblend.entity.CoalType;

public interface CoalTypeService {

    IPage<CoalType> page(long current, long size, String keyword);

    CoalType getById(Long id);

    void add(CoalType entity);

    void update(CoalType entity);

    void delete(Long id);
}
