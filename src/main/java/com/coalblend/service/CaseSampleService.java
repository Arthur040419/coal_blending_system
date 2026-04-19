package com.coalblend.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.coalblend.entity.CaseSample;

import java.util.List;

public interface CaseSampleService {

    IPage<CaseSample> page(long current, long size, Integer status, String keyword);

    List<CaseSample> search(String keyword);

    CaseSample getById(Long id);

    void add(CaseSample entity);

    void update(CaseSample entity);

    void delete(Long id);
}
