package com.coalblend.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.coalblend.common.result.Result;
import com.coalblend.entity.CoalType;
import com.coalblend.service.CoalTypeService;
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
@RequestMapping("/coalType")
@RequiredArgsConstructor
public class CoalTypeController {

    private final CoalTypeService coalTypeService;

    @GetMapping("/page")
    public Result<IPage<CoalType>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword) {
        return Result.ok(coalTypeService.page(current, size, keyword));
    }

    @GetMapping("/detail/{id}")
    public Result<CoalType> detail(@PathVariable Long id) {
        return Result.ok(coalTypeService.getById(id));
    }

    @PostMapping("/add")
    public Result<CoalType> add(@RequestBody CoalType body) {
        coalTypeService.add(body);
        return Result.ok(body);
    }

    @PutMapping("/update")
    public Result<Void> update(@RequestBody CoalType body) {
        coalTypeService.update(body);
        return Result.ok();
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        coalTypeService.delete(id);
        return Result.ok();
    }
}
