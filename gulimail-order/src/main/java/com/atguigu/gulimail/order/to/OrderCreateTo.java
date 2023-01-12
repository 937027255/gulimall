package com.atguigu.gulimail.order.to;

import com.atguigu.gulimail.order.entity.OrderEntity;
import com.atguigu.gulimail.order.entity.OrderItemEntity;
import lombok.Data;


import java.math.BigDecimal;
import java.util.List;

/**
 *  提交订单接口：创建的订单To对象
 *  订单
 *  订单项
 * @author zr
 * @date 2021/12/23 22:23
 */
@Data
public class OrderCreateTo {
    private OrderEntity order;  // 订单
    private List<OrderItemEntity> orderItems; // 订单项
    /** 订单计算的应付价格 **/
    private BigDecimal payPrice;
    /** 运费 **/
    private BigDecimal fare;
}
