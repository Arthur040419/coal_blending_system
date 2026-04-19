package com.coalblend.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.coalblend.common.result.Result;
import com.coalblend.entity.CoalQuality;
import com.coalblend.service.CoalQualityService;
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
@RequestMapping("/coalQuality")
@RequiredArgsConstructor
public class CoalQualityController {

    private final CoalQualityService coalQualityService;

    @GetMapping("/page")
    public Result<IPage<CoalQuality>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long coalId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        return Result.ok(coalQualityService.page(current, size, coalId, status, keyword));
    }

    @GetMapping("/listByCoal/{coalId}")
    public Result<List<CoalQuality>> listByCoal(@PathVariable Long coalId) {
        return Result.ok(coalQualityService.listByCoal(coalId));
    }

    @GetMapping("/latest/{coalId}")
    public Result<CoalQuality> latest(@PathVariable Long coalId) {
        return Result.ok(coalQualityService.latest(coalId));
    }

    @GetMapping("/detail/{id}")
    public Result<CoalQuality> detail(@PathVariable Long id) {
        return Result.ok(coalQualityService.getById(id));
    }

    @PostMapping("/add")
    public Result<CoalQuality> add(@RequestBody CoalQuality body) {
        coalQualityService.add(body);
        return Result.ok(body);
    }

    @PutMapping("/update")
    public Result<Void> update(@RequestBody CoalQuality body) {
        coalQualityService.update(body);
        return Result.ok();
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        coalQualityService.delete(id);
        return Result.ok();
    }
}
