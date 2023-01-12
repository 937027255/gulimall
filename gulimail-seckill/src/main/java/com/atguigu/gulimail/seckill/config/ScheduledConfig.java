package com.atguigu.gulimail.seckill.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling //开启定时任务
@EnableAsync //开启异步任务
@Configuration
public class ScheduledConfig {
    
}
