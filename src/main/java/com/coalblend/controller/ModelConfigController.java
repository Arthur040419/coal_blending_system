package com.coalblend.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.coalblend.common.result.Result;
import com.coalblend.dto.ModelConfigStatusDTO;
import com.coalblend.entity.ModelConfig;
import com.coalblend.service.ModelConfigService;
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

import java.util.List;

@RestController
@RequestMapping("/modelConfig")
@RequiredArgsConstructor
public class ModelConfigController {

    private final ModelConfigService modelConfigService;

    @GetMapping("/list")
    public Result<List<ModelConfig>> list() {
        return Result.ok(modelConfigService.listAll());
    }

    @GetMapping("/page")
    public Result<IPage<ModelConfig>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String modelType) {
        return Result.ok(modelConfigService.page(current, size, keyword, modelType));
    }

    @GetMapping("/detail/{id}")
    public Result<ModelConfig> detail(@PathVariable Long id) {
        return Result.ok(modelConfigService.getById(id));
    }

    @PostMapping("/add")
    public Result<ModelConfig> add(@RequestBody ModelConfig body) {
        modelConfigService.add(body);
        return Result.ok(body);
    }

    @PutMapping("/update")
    public Result<Void> update(@RequestBody ModelConfig body) {
        modelConfigService.update(body);
        return Result.ok();
    }

    @PutMapping("/status")
    public Result<Void> status(@RequestBody @Valid ModelConfigStatusDTO dto) {
        modelConfigService.updateStatus(dto.getId(), dto.getStatus());
        return Result.ok();
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        modelConfigService.delete(id);
        return Result.ok();
    }
}
