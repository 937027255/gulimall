package com.atguigu.gulimail.order.service;

import com.atguigu.common.mq.SeckillOrderTo;
import com.atguigu.gulimail.order.config.AlipayTemplate;
import com.atguigu.gulimail.order.vo.OrderConfirmVo;
import com.atguigu.gulimail.order.vo.OrderSubmitVo;
import com.atguigu.gulimail.order.vo.PayAsyncVo;
import com.atguigu.gulimail.order.vo.SubmitOrderResponseVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimail.order.entity.OrderEntity;

import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * ????
 *
 * @author zhangtianyu
 * @email 937027255@qq.com
 * @date 2022-07-08 20:13:28
 */
public interface OrderService extends IService<OrderEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * 将一个用户的地址信息和所有的购物项信息查询并封装返回
     * @return
     */
    OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException;

    SubmitOrderResponseVo submitOrder(OrderSubmitVo orderSubmitVo);

    OrderEntity getOrderStatus(String orderSn);

    void closeOrder(OrderEntity orderEntity);

    AlipayTemplate.PayVo getOrderPay(String orderSn);

    PageUtils queryPageWithItem(Map<String, Object> params);

    String handlePayResult(PayAsyncVo vo);

    void updateOrderStauts(String orderSn, Integer code);

    void createSeckillOrder(SeckillOrderTo seckillOrderTo);
}

