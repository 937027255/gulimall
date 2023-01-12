package com.atguigu.gulimail.order;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 使用RabbitMQ
 * 1、引入amqp场景，RabbitAutoConfiguration就会自动生效
 * 2、给容器中自动配置了：RabbitTemplate、CachingConnectionFactory、AmqpAdmin、RabbitMessagingTemplate
 * 3、启动@EnableRabbit
 */
@EnableRabbit
@EnableFeignClients
@EnableDiscoveryClient
@SpringBootApplication
@MapperScan("com.atguigu.gulimail.order.dao")
public class GulimailOrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimailOrderApplication.class, args);
    }

}
