package com.coalblend.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.coalblend.common.result.Result;
import com.coalblend.dto.OrderStatusDTO;
import com.coalblend.entity.Orders;
import com.coalblend.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/page")
    public Result<IPage<Orders>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String orderStatus) {
        return Result.ok(orderService.page(current, size, keyword, orderStatus));
    }

    @GetMapping("/detail/{id}")
    public Result<Orders> detail(@PathVariable Long id) {
        return Result.ok(orderService.getById(id));
    }

    @PostMapping("/add")
    public Result<Orders> add(@RequestBody Orders body) {
        orderService.add(body);
        return Result.ok(body);
    }

    @PutMapping("/update")
    public Result<Void> update(@RequestBody Orders body) {
        orderService.update(body);
        return Result.ok();
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        orderService.delete(id);
        return Result.ok();
    }

    @PutMapping("/status")
    public Result<Void> status(@RequestBody @Valid OrderStatusDTO dto) {
        orderService.updateStatus(dto.getId(), dto.getOrderStatus());
        return Result.ok();
    }
}
