package com.atguigu.gulimail.order.mvo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderConfirmVo {
    //收货地址
    List<MemberAdressVo> address;
    //选中的所有购物项
    List<OrderItemVo> items;
    //优惠卷信息
    Integer integration;
    //订单总额
    BigDecimal total;
    //应付价格
    BigDecimal payPrice;
}
