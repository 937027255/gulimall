package com.atguigu.gulimail.product.config;

import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import org.mapstruct.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
//开启mybaitisplus的事务注解
@EnableTransactionManagement
@MapperScan("com.atguigu.gulimail.product.dao")
public class MybatisConfig {


    /*分页插件*/
    @Bean
    public PaginationInterceptor paginationInterceptor(){
        return new PaginationInterceptor();
    }


}
