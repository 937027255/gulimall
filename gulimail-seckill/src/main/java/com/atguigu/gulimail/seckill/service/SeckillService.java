package com.atguigu.gulimail.seckill.service;

import com.atguigu.gulimail.seckill.vo.SeckillRelationVoWithSkuInfo;

import java.util.List;

public interface SeckillService {

    void upload3DaysSku();

    List<SeckillRelationVoWithSkuInfo> getCurrentSeckillSkus();

    SeckillRelationVoWithSkuInfo getSeckillSkuById(Long skuId);

    String killSku(String killId, String key, Integer num);
}
