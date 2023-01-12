package com.atguigu.gulimail.seckill.scheduled;

import com.atguigu.gulimail.seckill.service.SeckillService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 秒杀商品的定时上架
 * 每天晚上3点上架最近三天需要秒杀的商品
 */
@Slf4j
@Service
public class SeckillSkuScheduled {

    @Autowired
    SeckillService seckillService;
    @Autowired
    RedissonClient redissonClient;

    private final String UPLOAD_LOCK = "seckill:upload:lock";
    /**
     * 上架最近三天要参加秒杀的商品
     * 这个定时任务需要上锁，否则多个任务可能会同时进行，获取所得机器才能执行上架操作
     */
    @Scheduled(cron = "0/3 * * * * ?")
    public void uploadSeckillSkuLastest3Days() {
        System.out.println("上架秒杀的商品信息");
        RLock lock = redissonClient.getLock(UPLOAD_LOCK);
        //加入多个同时执行上架逻辑，只会放进去一个，后面的再进去的时候接口是幂等的所以不会重复上架
        lock.lock(10, TimeUnit.SECONDS);
        try {
            seckillService.upload3DaysSku();
        } finally {
            lock.unlock();
        }
    }
}
