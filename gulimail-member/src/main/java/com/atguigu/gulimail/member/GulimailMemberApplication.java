package com.atguigu.gulimail.member;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@MapperScan("com.atguigu.gulimail.member.dao")
@ComponentScan("com.atguigu")
@EnableDiscoveryClient
@EnableFeignClients
public class GulimailMemberApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimailMemberApplication.class, args);
    }

}
