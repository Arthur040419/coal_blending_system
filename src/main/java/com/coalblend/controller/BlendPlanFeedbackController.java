package com.coalblend.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.coalblend.common.result.Result;
import com.coalblend.entity.BlendPlanFeedback;
import com.coalblend.service.BlendPlanFeedbackService;
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
@RequestMapping("/blendPlanFeedback")
@RequiredArgsConstructor
public class BlendPlanFeedbackController {

    private final BlendPlanFeedbackService feedbackService;

    @GetMapping("/page")
    public Result<IPage<BlendPlanFeedback>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long planId,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) Integer qualifiedFlag) {
        return Result.ok(feedbackService.page(current, size, planId, orderId, qualifiedFlag));
    }

    @GetMapping("/byPlan/{planId}")
    public Result<List<BlendPlanFeedback>> byPlan(@PathVariable Long planId) {
        return Result.ok(feedbackService.listByPlan(planId));
    }

    @GetMapping("/detail/{id}")
    public Result<BlendPlanFeedback> detail(@PathVariable Long id) {
        return Result.ok(feedbackService.getById(id));
    }

    @PostMapping("/add")
    public Result<BlendPlanFeedback> add(@RequestBody BlendPlanFeedback feedback) {
        feedbackService.add(feedback);
        return Result.ok(feedback);
    }

    @PutMapping("/update")
    public Result<Void> update(@RequestBody BlendPlanFeedback feedback) {
        feedbackService.update(feedback);
        return Result.ok();
    }

    @PostMapping("/convertToCase/{feedbackId}")
    public Result<Long> convertToCase(@PathVariable Long feedbackId) {
        return Result.ok(feedbackService.convertToCase(feedbackId));
    }
}
