package com.coalblend.controller;

import com.coalblend.common.result.Result;
import com.coalblend.dto.LoginDTO;
import com.coalblend.service.AuthService;
import com.coalblend.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<UserVO> login(@RequestBody @Valid LoginDTO dto) {
        return Result.ok(authService.login(dto));
    }

    @GetMapping("/me")
    public Result<UserVO> me(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return Result.ok(authService.currentUser(userId));
    }
}
