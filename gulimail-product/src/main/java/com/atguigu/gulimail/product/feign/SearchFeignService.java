package com.atguigu.gulimail.product.feign;

import com.atguigu.common.to.es.SkuEsModel;
import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient("gulimail-search")
public interface SearchFeignService {
    @PostMapping("/es/product/save")
    public R saveProduct(@RequestBody List<SkuEsModel> esModels);
}
