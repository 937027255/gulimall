package com.atguigu.gulimail.product.feign;

import com.atguigu.common.to.SkuStockVo;
import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient("gulimail-ware")
public interface WareFeignService {
    @PostMapping("/ware/waresku/hasStock")
    public R<List<SkuStockVo>> getSkuHasStock(@RequestBody List<Long> skuIds);
}
