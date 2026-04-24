package com.coalblend.common.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class LlmClientConfiguration {

    @Bean
    @Qualifier("llmRestTemplate")
    public RestTemplate llmRestTemplate(RestTemplateBuilder builder, CoalLlmProperties props) {
        return builder
                .setConnectTimeout(props.getConnectTimeout())
                .setReadTimeout(props.getReadTimeout())
                .build();
    }
}
