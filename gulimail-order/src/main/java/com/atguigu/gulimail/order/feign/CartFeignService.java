package com.atguigu.gulimail.order.feign;

import com.atguigu.gulimail.order.vo.OrderItemVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient("gulimail-cart")
public interface CartFeignService {
    @GetMapping("/{userId}")
    public List<OrderItemVo> getCarsByUserId(@PathVariable Long userId);
}
