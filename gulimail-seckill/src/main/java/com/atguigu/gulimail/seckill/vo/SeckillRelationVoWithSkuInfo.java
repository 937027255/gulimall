package com.atguigu.gulimail.seckill.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SeckillRelationVoWithSkuInfo {
    private Long id;

    private Long promotionId;

    private Long promotionSessionId;

    private Long skuId;

    private BigDecimal seckillPrice;

    private BigDecimal seckillCount;

    private BigDecimal seckillLimit;

    private Integer seckillSort;

    //商品详情信息
    private SkuinfoVo skuInfo;

    //开始时间
    private Long start;

    //结束时间
    private Long end;

    //随机码
    private String randomCode;
}
