package com.coalblend.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.coalblend.common.result.Result;
import com.coalblend.dto.BlendGenerateDTO;
import com.coalblend.dto.BlendPlanSelectDTO;
import com.coalblend.entity.BlendPlan;
import com.coalblend.entity.BlendPlanDetail;
import com.coalblend.service.BlendPlanService;
import com.coalblend.vo.blend.BlendGenerateResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/blendPlan")
@RequiredArgsConstructor
public class BlendPlanController {

    private final BlendPlanService blendPlanService;

    @PostMapping("/generate")
    public Result<BlendGenerateResultVO> generate(@RequestBody @Valid BlendGenerateDTO dto) {
        return Result.ok(blendPlanService.generate(dto));
    }

    @GetMapping("/page")
    public Result<IPage<BlendPlan>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) String planStatus,
            @RequestParam(required = false) String planCode,
            @RequestParam(required = false) String orderCode,
            @RequestParam(required = false) String createTimeBegin,
            @RequestParam(required = false) String createTimeEnd) {
        return Result.ok(blendPlanService.page(current, size, orderId, planStatus, planCode, orderCode,
                createTimeBegin, createTimeEnd));
    }

    @GetMapping("/history")
    public Result<IPage<BlendPlan>> history(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) String planStatus,
            @RequestParam(required = false) String planCode,
            @RequestParam(required = false) String orderCode,
            @RequestParam(required = false) String createTimeBegin,
            @RequestParam(required = false) String createTimeEnd) {
        return Result.ok(blendPlanService.page(current, size, orderId, planStatus, planCode, orderCode,
                createTimeBegin, createTimeEnd));
    }

    @GetMapping("/detail/{id}")
    public Result<BlendPlan> detail(@PathVariable Long id) {
        return Result.ok(blendPlanService.getById(id));
    }

    @GetMapping("/details/{planId}")
    public Result<List<BlendPlanDetail>> details(@PathVariable Long planId) {
        return Result.ok(blendPlanService.listDetails(planId));
    }

    @GetMapping("/byOrder/{orderId}")
    public Result<List<BlendPlan>> byOrder(@PathVariable Long orderId) {
        return Result.ok(blendPlanService.listByOrder(orderId));
    }

    @PutMapping("/select")
    public Result<Void> select(@RequestBody @Valid BlendPlanSelectDTO dto) {
        blendPlanService.selectPlan(dto.getPlanId());
        return Result.ok();
    }
}
