package com.coalblend.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.coalblend.common.result.Result;
import com.coalblend.entity.RuleKnowledge;
import com.coalblend.service.RuleKnowledgeService;
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
@RequestMapping("/ruleKnowledge")
@RequiredArgsConstructor
public class RuleKnowledgeController {

    private final RuleKnowledgeService ruleKnowledgeService;

    @GetMapping("/page")
    public Result<IPage<RuleKnowledge>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String ruleType,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        return Result.ok(ruleKnowledgeService.page(current, size, ruleType, status, keyword));
    }

    @GetMapping("/byType")
    public Result<List<RuleKnowledge>> byType(@RequestParam String ruleType) {
        return Result.ok(ruleKnowledgeService.listByType(ruleType));
    }

    @GetMapping("/enabled")
    public Result<List<RuleKnowledge>> enabled() {
        return Result.ok(ruleKnowledgeService.listEnabled());
    }

    @GetMapping("/detail/{id}")
    public Result<RuleKnowledge> detail(@PathVariable Long id) {
        return Result.ok(ruleKnowledgeService.getById(id));
    }

    @PostMapping("/add")
    public Result<RuleKnowledge> add(@RequestBody RuleKnowledge body) {
        ruleKnowledgeService.add(body);
        return Result.ok(body);
    }

    @PutMapping("/update")
    public Result<Void> update(@RequestBody RuleKnowledge body) {
        ruleKnowledgeService.update(body);
        return Result.ok();
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        ruleKnowledgeService.delete(id);
        return Result.ok();
    }
}
