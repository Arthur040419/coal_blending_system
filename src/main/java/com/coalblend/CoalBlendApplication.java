package com.coalblend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.coalblend.mapper")
public class CoalBlendApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoalBlendApplication.class, args);
    }
}
