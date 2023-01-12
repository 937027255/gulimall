package com.atguigu.gulimail.order.vo;

import lombok.Data;

import java.util.List;

/**
 * 锁定库存的vo
 * 创建订单的时候，封装的数据
 * @author zr
 * @date 2021/12/23 22:30
 */
@Data
public class WareSkuLockVo {
    private String orderSn;
    /** 需要锁住的所有库存信息 **/
    private List<OrderItemVo> locks;
}
