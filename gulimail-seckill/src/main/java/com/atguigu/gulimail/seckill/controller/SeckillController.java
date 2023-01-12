package com.atguigu.gulimail.seckill.controller;

import com.atguigu.common.utils.R;
import com.atguigu.gulimail.seckill.service.SeckillService;
import com.atguigu.gulimail.seckill.vo.SeckillRelationVoWithSkuInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SeckillController {

    @Autowired
    SeckillService seckillService;

    /**
     * 获取当前时刻能够参与秒杀的商品
     * @return
     */
    @GetMapping("/currentSeckillSkus")
    public R getCurrentSeckillSkus() {
        List<SeckillRelationVoWithSkuInfo> redisSkus =  seckillService.getCurrentSeckillSkus();
        return R.ok().setData(redisSkus);
    }

    @GetMapping("/seckillSkuById/{skuId}")
    public R getSeckillSkuById(@PathVariable("skuId") Long skuId) {
        SeckillRelationVoWithSkuInfo skuInfo =  seckillService.getSeckillSkuById(skuId);
        return R.ok().setData(skuInfo);
    }

    @GetMapping("/kill")
    public R killSku(@RequestParam("killId") String killId,@RequestParam("key") String key,@RequestParam("num") Integer num) {
        String orderSn = seckillService.killSku(killId,key,num);
        return R.ok().setData(orderSn);
    }
}
