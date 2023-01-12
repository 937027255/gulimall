package com.atguigu.gulimail.order.feign;

import com.atguigu.common.to.SkuStockVo;
import com.atguigu.common.utils.R;
import com.atguigu.gulimail.order.vo.WareSkuLockVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient("gulimail-ware")
public interface WareFeignService {
    @PostMapping("/ware/waresku/hasStock")
    public R<List<SkuStockVo>> getSkuHasStock(@RequestBody List<Long> skuIds);
    @GetMapping("/ware/wareinfo/fare")
    public R fare(@RequestParam("addrId") Long addrId);
    @PostMapping("/ware/waresku/lock/order")
    public R orderLockStock(@RequestBody WareSkuLockVo vo);
}
