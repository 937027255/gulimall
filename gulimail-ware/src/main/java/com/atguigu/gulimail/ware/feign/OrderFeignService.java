package com.atguigu.gulimail.ware.feign;

import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("gulimail-order")
public interface OrderFeignService {
    @GetMapping("/order/order/status/{orderSn}")
    public R getOrderStatus(@PathVariable String orderSn);
}
