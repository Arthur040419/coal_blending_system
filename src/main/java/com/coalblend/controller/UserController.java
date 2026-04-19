package com.coalblend.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.coalblend.common.result.Result;
import com.coalblend.dto.UserStatusDTO;
import com.coalblend.entity.SysUser;
import com.coalblend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/page")
    public Result<IPage<SysUser>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Integer status) {
        return Result.ok(userService.page(current, size, keyword, role, status));
    }

    @PostMapping("/add")
    public Result<SysUser> add(@RequestBody SysUser body) {
        userService.add(body);
        return Result.ok(body);
    }

    @PutMapping("/update")
    public Result<Void> update(@RequestBody SysUser body) {
        userService.update(body);
        return Result.ok();
    }

    @PutMapping("/status")
    public Result<Void> status(@RequestBody @Valid UserStatusDTO dto) {
        userService.updateStatus(dto.getId(), dto.getStatus());
        return Result.ok();
    }
}
