package com.coalblend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coalblend.common.exception.BusinessException;
import com.coalblend.entity.Orders;
import com.coalblend.mapper.OrdersMapper;
import com.coalblend.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrdersMapper ordersMapper;

    @Override
    public IPage<Orders> page(long current, long size, String keyword, String orderStatus) {
        Page<Orders> page = new Page<>(current, size);
        LambdaQueryWrapper<Orders> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            w.and(q -> q.like(Orders::getOrderCode, keyword).or().like(Orders::getCustomerName, keyword));
        }
        if (StringUtils.hasText(orderStatus)) {
            w.eq(Orders::getOrderStatus, orderStatus);
        }
        w.orderByDesc(Orders::getId);
        return ordersMapper.selectPage(page, w);
    }

    @Override
    public Orders getById(Long id) {
        Orders row = ordersMapper.selectById(id);
        if (row == null) {
            throw new BusinessException(404, "订单不存在");
        }
        return row;
    }

    @Override
    public void add(Orders entity) {
        Long cnt = ordersMapper.selectCount(
                new LambdaQueryWrapper<Orders>().eq(Orders::getOrderCode, entity.getOrderCode()));
        if (cnt != null && cnt > 0) {
            throw new BusinessException("订单编号已存在");
        }
        if (entity.getOrderStatus() == null) {
            entity.setOrderStatus("pending");
        }
        ordersMapper.insert(entity);
    }

    @Override
    public void update(Orders entity) {
        if (entity.getId() == null) {
            throw new BusinessException("id 不能为空");
        }
        getById(entity.getId());
        if (StringUtils.hasText(entity.getOrderCode())) {
            Long cnt = ordersMapper.selectCount(new LambdaQueryWrapper<Orders>()
                    .eq(Orders::getOrderCode, entity.getOrderCode())
                    .ne(Orders::getId, entity.getId()));
            if (cnt != null && cnt > 0) {
                throw new BusinessException("订单编号已存在");
            }
        }
        ordersMapper.updateById(entity);
    }

    @Override
    public void delete(Long id) {
        getById(id);
        ordersMapper.deleteById(id);
    }

    @Override
    public void updateStatus(Long id, String orderStatus) {
        Orders o = new Orders();
        o.setId(id);
        o.setOrderStatus(orderStatus);
        getById(id);
        ordersMapper.updateById(o);
    }
}
