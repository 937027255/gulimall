package com.atguigu.gulimail.seckill.feign;

import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient("gulimail-coupon")
public interface CouponFeignService {

    @GetMapping("/coupon/seckillsession/listSeckillOf3Days")
    public R listSeckillOf3Days();
}
