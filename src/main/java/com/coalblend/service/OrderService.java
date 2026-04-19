package com.coalblend.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.coalblend.entity.Orders;

public interface OrderService {

    IPage<Orders> page(long current, long size, String keyword, String orderStatus);

    Orders getById(Long id);

    void add(Orders entity);

    void update(Orders entity);

    void delete(Long id);

    void updateStatus(Long id, String orderStatus);
}
