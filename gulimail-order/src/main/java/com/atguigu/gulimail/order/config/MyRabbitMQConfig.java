package com.atguigu.gulimail.order.config;

/**
 * @author zr
 * @date 2021/12/28 16:47
 */

import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.HashMap;

/**
 * 创建队列，交换机，延迟队列，绑定关系 的configuration
 * 不会重复创建覆盖
 * 1、第一次使用队列【监听】的时候才会创建
 * 2、Broker没有队列、交换机才会创建
 */
@Configuration
public class MyRabbitMQConfig {

//    @RabbitListener(queues = "order.release.order.queue")
//    public void listen(OrderEntity orderEntity, Channel channel, Message message) throws IOException {
//        System.out.println("收到过期订单消息,准备关闭订单:------>"+orderEntity.getOrderSn());
//        channel.basicNack(message.getMessageProperties().getDeliveryTag(),false,false);
//    }
    /**
     * 延时队列
     * @return
     */
    @Bean
    public Queue orderDelayQueue(){
        HashMap<String, Object> arguments = new HashMap<>();
        arguments.put("x-dead-letter-exchange", "order-event-exchange");// 死信路由
        arguments.put("x-dead-letter-routing-key", "order.release.order");// 死信路由键
        arguments.put("x-message-ttl", 30000); // 消息过期时间 1分钟
        /*
            Queue(String name,  队列名字
            boolean durable,  是否持久化
            boolean exclusive,  是否排他
            boolean autoDelete, 是否自动删除
            Map<String, Object> arguments) 属性【TTL、死信路由、死信路由键】
         */
        Queue queue = new Queue("order.delay.queue",true,false,false,arguments);
        return queue;
    }

    /**
     * 死信队列
     * @return
     */
    @Bean
    public Queue orderReleaseQueue(){
        Queue queue = new Queue("order.release.order.queue",true,false,false);
        return queue;
    }

    /**
     * 死信路由[普通路由]
     * @return
     */
    @Bean
    public Exchange orderEventExchange(){
        /*
         *   String name,
         *   boolean durable,
         *   boolean autoDelete,
         *   Map<String, Object> arguments
         * */
        TopicExchange topicExchange = new TopicExchange("order-event-exchange",true,false);

        return topicExchange;
    }

    /**
     * 交换机与延时队列的绑定
     * @return
     */
    @Bean
    public Binding orderCreateOrderBinding(){
        /*
         * String destination, 目的地（队列名或者交换机名字）
         * DestinationType destinationType, 目的地类型（Queue、Exhcange）
         * String exchange,
         * String routingKey,
         * Map<String, Object> arguments
         * */
        Binding binding = new Binding("order.delay.queue",
                Binding.DestinationType.QUEUE,
                "order-event-exchange",
                "order.create.order",
                null);
        return binding;
    }

    /**
     * 死信路由与普通死信队列的绑定
     * @return
     */
    @Bean
    public Binding orderReleaseOrderBinding(){
        Binding binding = new Binding("order.release.order.queue",
                Binding.DestinationType.QUEUE,
                "order-event-exchange",
                "order.release.order",
                null);
        return binding;
    }




    /**
     * 商品秒杀队列
     * 作用：削峰，创建订单
     */
    @Bean
    public Queue orderSecKillOrderQueue() {
        Queue queue = new Queue("order.seckill.order.queue", true, false, false);
        return queue;
    }

    @Bean
    public Binding orderSecKillOrderQueueBinding() {
        //String destination, DestinationType destinationType, String exchange, String routingKey,
        // 			Map<String, Object> arguments
        Binding binding = new Binding(
                "order.seckill.order.queue",
                Binding.DestinationType.QUEUE,
                "order-event-exchange",
                "order.seckill.order",
                null);

        return binding;
    }
    @Bean
    public Binding orderReleaseOtherBinding() {
        Binding binding = new Binding(
                "stock.release.stock.queue",
                Binding.DestinationType.QUEUE,
                "order-event-exchange",
                "order.release.other.#",
                null);

        return binding;
    }
}
