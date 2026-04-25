package com.coalblend;

import com.coalblend.common.config.CoalLlmProperties;
import com.coalblend.common.config.CoalBlendProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@MapperScan("com.coalblend.mapper")
@EnableConfigurationProperties({CoalLlmProperties.class, CoalBlendProperties.class})
public class CoalBlendApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoalBlendApplication.class, args);
    }
}
