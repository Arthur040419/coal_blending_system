package com.coalblend.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.coalblend.entity.CoalQuality;

import java.util.List;

public interface CoalQualityService {

    IPage<CoalQuality> page(long current, long size, Long coalId, Integer status, String keyword);

    List<CoalQuality> listByCoal(Long coalId);

    CoalQuality latest(Long coalId);

    CoalQuality getById(Long id);

    void add(CoalQuality entity);

    void update(CoalQuality entity);

    void delete(Long id);
}
