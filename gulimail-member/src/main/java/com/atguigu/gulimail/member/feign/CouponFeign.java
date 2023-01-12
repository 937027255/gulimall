package com.atguigu.gulimail.member.feign;

import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;

@Component
@FeignClient("gulimail-coupon")
public interface CouponFeign {

    @RequestMapping("/coupon/coupon/feign/test")
    public R membercoupons();
}
