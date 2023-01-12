package com.atguigu.common.mq;

import lombok.Data;

@Data
public class StockLockedTo {
    private Long id;//库存工作单的id
    private StockDetailTo detailTo;//这个购物项的详情库存锁定信息
}
