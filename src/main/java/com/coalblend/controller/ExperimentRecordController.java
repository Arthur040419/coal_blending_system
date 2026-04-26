package com.coalblend.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.coalblend.common.result.Result;
import com.coalblend.entity.ExperimentRecord;
import com.coalblend.service.ExperimentRecordService;
import com.coalblend.vo.experiment.ExperimentRadarVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/experimentRecord")
@RequiredArgsConstructor
public class ExperimentRecordController {

    private final ExperimentRecordService experimentRecordService;

    @GetMapping("/page")
    public Result<IPage<ExperimentRecord>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String experimentCode,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) String modelName,
            @RequestParam(required = false) Long planId) {
        return Result.ok(experimentRecordService.page(current, size, experimentCode, orderId, modelName, planId));
    }

    @GetMapping("/byOrder/{orderId}")
    public Result<List<ExperimentRecord>> byOrder(@PathVariable Long orderId) {
        return Result.ok(experimentRecordService.listByOrder(orderId));
    }

    @GetMapping("/radar")
    public Result<ExperimentRadarVO> radar(
            @RequestParam(required = false) String experimentCode,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) String modelName) {
        return Result.ok(experimentRecordService.radar(experimentCode, orderId, modelName));
    }
}
