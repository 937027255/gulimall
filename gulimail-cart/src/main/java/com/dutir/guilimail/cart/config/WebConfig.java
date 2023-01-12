package com.dutir.guilimail.cart.config;

import com.dutir.guilimail.cart.interceptor.GulimailInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
//        registry.addViewController("cartList.html").setViewName("cartList");
//        registry.addViewController("success.html").setViewName("success");
    }

    /**
     * 拦截所有的请求进入到自定义的拦截器中去
     * @param registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new GulimailInterceptor()).addPathPatterns("/**");
    }
}
