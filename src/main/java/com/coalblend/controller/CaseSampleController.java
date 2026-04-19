package com.coalblend.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.coalblend.common.result.Result;
import com.coalblend.entity.CaseSample;
import com.coalblend.service.CaseSampleService;
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
@RequestMapping("/caseSample")
@RequiredArgsConstructor
public class CaseSampleController {

    private final CaseSampleService caseSampleService;

    @GetMapping("/page")
    public Result<IPage<CaseSample>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        return Result.ok(caseSampleService.page(current, size, status, keyword));
    }

    @GetMapping("/search")
    public Result<List<CaseSample>> search(@RequestParam(required = false) String keyword) {
        return Result.ok(caseSampleService.search(keyword));
    }

    @GetMapping("/detail/{id}")
    public Result<CaseSample> detail(@PathVariable Long id) {
        return Result.ok(caseSampleService.getById(id));
    }

    @PostMapping("/add")
    public Result<CaseSample> add(@RequestBody CaseSample body) {
        caseSampleService.add(body);
        return Result.ok(body);
    }

    @PutMapping("/update")
    public Result<Void> update(@RequestBody CaseSample body) {
        caseSampleService.update(body);
        return Result.ok();
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        caseSampleService.delete(id);
        return Result.ok();
    }
}
