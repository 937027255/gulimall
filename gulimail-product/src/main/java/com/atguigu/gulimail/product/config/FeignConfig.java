package com.atguigu.gulimail.product.config;

import feign.Request;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {
    @Bean
    public Request.Options feignRequestOptions() {
        return new Request.Options(6000,600000);
    }

}
