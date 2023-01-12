package com.atguigu.common.to;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SkuLadderTo {

    private Long skuId;
    private BigDecimal fullCount;
    private BigDecimal discount;
    private BigDecimal price;
    private int addOther;
}
