package com.coalblend.controller;

import com.coalblend.common.result.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/health")
public class HealthController {

    @Value("${spring.application.name}")
    private String applicationName;

    @GetMapping
    public Result<Map<String, String>> health() {
        return Result.ok(Map.of(
                "status", "UP",
                "application", applicationName
        ));
    }
}
