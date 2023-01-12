package com.atguigu.gulimail.product.feign;

import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("gulimail-seckill")
public interface SeckillFeignSerive {
    @GetMapping("/seckillSkuById/{skuId}")
    public R getSeckillSkuById(@PathVariable("skuId") Long skuId);
}
