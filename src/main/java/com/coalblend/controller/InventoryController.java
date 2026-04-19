package com.coalblend.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.coalblend.common.result.Result;
import com.coalblend.entity.Inventory;
import com.coalblend.service.InventoryService;
import com.coalblend.vo.InventoryAvailableVO;
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

import java.util.List;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/page")
    public Result<IPage<Inventory>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long coalId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String warehouseCode) {
        return Result.ok(inventoryService.page(current, size, coalId, status, warehouseCode));
    }

    @GetMapping("/byCoal/{coalId}")
    public Result<List<Inventory>> byCoal(@PathVariable Long coalId) {
        return Result.ok(inventoryService.listByCoal(coalId));
    }

    @GetMapping("/available/{coalId}")
    public Result<InventoryAvailableVO> available(@PathVariable Long coalId) {
        return Result.ok(inventoryService.availableSummary(coalId));
    }

    @GetMapping("/detail/{id}")
    public Result<Inventory> detail(@PathVariable Long id) {
        return Result.ok(inventoryService.getById(id));
    }

    @PostMapping("/add")
    public Result<Inventory> add(@RequestBody Inventory body) {
        inventoryService.add(body);
        return Result.ok(body);
    }

    @PutMapping("/update")
    public Result<Void> update(@RequestBody Inventory body) {
        inventoryService.update(body);
        return Result.ok();
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        inventoryService.delete(id);
        return Result.ok();
    }
}
