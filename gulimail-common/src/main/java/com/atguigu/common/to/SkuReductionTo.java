package com.atguigu.common.to;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SkuReductionTo {

    private Long skuId;
    private BigDecimal fullPrice;
    private BigDecimal reducePrice;
    private int addOther;
}
