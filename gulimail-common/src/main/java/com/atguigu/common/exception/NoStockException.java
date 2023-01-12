package com.atguigu.common.exception;

public class NoStockException extends RuntimeException{
    private Long skuId;
    public NoStockException(Long skuId) {
        super("库存不足:" + skuId);
    }

    public Long getSkuId() {
        return skuId;
    }
    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }
}
