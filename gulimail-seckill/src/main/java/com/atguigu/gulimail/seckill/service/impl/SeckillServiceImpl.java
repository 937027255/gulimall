package com.atguigu.gulimail.seckill.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.mq.SeckillOrderTo;
import com.atguigu.common.to.session.MemberResponseVo;
import com.atguigu.common.utils.R;
import com.atguigu.gulimail.seckill.feign.CouponFeignService;
import com.atguigu.gulimail.seckill.feign.ProductFeignSerivce;
import com.atguigu.gulimail.seckill.interceptor.SeckillInterceptor;
import com.atguigu.gulimail.seckill.service.SeckillService;
import com.atguigu.gulimail.seckill.vo.SeckillRelationVoWithSkuInfo;
import com.atguigu.gulimail.seckill.vo.SeckillSessionWithSkus;
import com.atguigu.gulimail.seckill.vo.SeckillSkuRelationVo;
import com.atguigu.gulimail.seckill.vo.SkuinfoVo;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SeckillServiceImpl implements SeckillService {

    @Autowired
    CouponFeignService couponFeignService;
    @Autowired
    StringRedisTemplate redisTemplate;
    @Autowired
    ProductFeignSerivce productFeignSerivce;
    @Autowired
    RedissonClient redissonClient;
    @Autowired
    RabbitTemplate rabbitTemplate;

    public static final String SESSIONS_CACHE_PREFIX = "seckill:sessions:";
    public static final String SKUKILL_CACHE_PREFIX = "seckill:skus:";
    public static final String SKU_STOCK_SEMAPHONE = "seckill:stock:";//+商品随机码

    @Override
    public void upload3DaysSku() {
        R r = couponFeignService.listSeckillOf3Days();
        if (r.getCode() == 0) {
            List<SeckillSessionWithSkus> sessionData = (List<SeckillSessionWithSkus>) r.getData(new TypeReference<List<SeckillSessionWithSkus>>() {});
            if (sessionData != null) {
                //如果这个时刻有三天内要参加秒杀的商品才运行下面的逻辑

                //将活动信息和商品的详细信息放入到redis
                //活动信息的key是商品的开始和结束日期，value是对应的skuId
                saveSessionInfos(sessionData);
                //活动的关联商品信息
                saveSessionSkuInfos(sessionData);
            }
        }
    }

    /**
     * 找到当前在redis中参与上架的所有sku的内容
     * @return
     */
    @Override
    public List<SeckillRelationVoWithSkuInfo> getCurrentSeckillSkus() {
        //这里都走redis不走数据库
        //首先根据当前的时间找到对应的场次信息
        //先模糊匹配查询到所有然后再根据时间进行判断
        String SearchKey = SESSIONS_CACHE_PREFIX + "*";
        Set<String> keys = redisTemplate.keys(SearchKey);
        if (keys != null) {
            for (String key : keys) {
                long time = new Date().getTime();
                String replace = key.replace(SESSIONS_CACHE_PREFIX, "");
                String[] s = replace.split("_");
                long start = Long.parseLong(s[0]);
                long end = Long.parseLong(s[1]);
                if (time >= start && time <= end) {
                    //如果在秒杀活动的区间内，就可以使用这个活动的session_id
                    //key对应的value是1_1
                    List<String> list = redisTemplate.opsForList().range(key, -100, 100);
                    BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
                    //直接获取多个key的value值封装成list返回
                    List<String> strings = hashOps.multiGet(list);
                    if (strings != null) {
                        List<SeckillRelationVoWithSkuInfo> collect = strings.stream().map(item -> {
                            SeckillRelationVoWithSkuInfo redisTo = JSON.parseObject(item, SeckillRelationVoWithSkuInfo.class);
                            return redisTo;
                        }).collect(Collectors.toList());
                        return collect;
                    }
                    break;
                }
            }
        }
        return null;
    }

    /**
     * 远程调用pms模块商品获取skuId对应的秒杀信息
     * @param skuId
     * @return
     */
    @Override
    public SeckillRelationVoWithSkuInfo getSeckillSkuById(Long skuId) {
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
        Set<String> keys = hashOps.keys();

        if (keys != null && keys.size() >0) {
            String regx = "\\d_" + skuId.toString();
            for (String key : keys) {
                if (Pattern.matches(regx,key)) {
                    //如果匹配成功，就查出来对应的内容
                    String s = hashOps.get(key);
                    SeckillRelationVoWithSkuInfo skuInfo = JSON.parseObject(s, SeckillRelationVoWithSkuInfo.class);
                    Long start = skuInfo.getStart();
                    Long end = skuInfo.getEnd();
                    long time = new Date().getTime();
                    if (time >= start && time <= end) {

                    } else {
                        skuInfo.setRandomCode(null);
                    }
                    return skuInfo;
                }
            }

        }
        return null;
    }

    /**
     * 用户秒杀逻辑
     * @param killId
     * @param key
     * @param num
     * @return
     */
    @Override
    public String killSku(String killId, String key,Integer num) {
        MemberResponseVo responseVo = SeckillInterceptor.loginUser.get();
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
        String json = hashOps.get(killId);
        if (json == null) {
            return null;
        } else {
            //有这个商品的秒杀信息
            //验证秒杀时间
            SeckillRelationVoWithSkuInfo skuSeckillInfo = JSON.parseObject(json, SeckillRelationVoWithSkuInfo.class);
            long time = new Date().getTime();
            Long start = skuSeckillInfo.getStart();
            Long end = skuSeckillInfo.getEnd();
            long diff = end - time;
            if (time >= start && time <= end) {
                //在秒杀的活动时间内
                //校验skuId和随机码是否正确
                String skuId = skuSeckillInfo.getPromotionSessionId().toString() + "_" + skuSeckillInfo.getSkuId().toString();
                String randomCode = skuSeckillInfo.getRandomCode();
                if (killId.equals(skuId) && key.equals(randomCode)) {
                    //验证购买数量
                    if (num <= skuSeckillInfo.getSeckillLimit().intValue()) {
                        //当次购买的数量满足要求
                        String userBuyInfo = responseVo.getId().toString() + "_" + skuId;
                        Boolean firstBuy = redisTemplate.opsForValue().setIfAbsent(userBuyInfo, num.toString(), diff, TimeUnit.MILLISECONDS);
                        if (firstBuy) {
                            //第一次购买
                            RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHONE + randomCode);
                            boolean b = semaphore.tryAcquire(num);
                            if (b) {
                                //秒杀成功
                                //给MQ发送秒杀成功的消息
                                String timeId = IdWorker.getTimeId();
                                SeckillOrderTo seckillOrderTo = new SeckillOrderTo();
                                seckillOrderTo.setOrderSn(timeId);
                                seckillOrderTo.setSeckillPrice(skuSeckillInfo.getSeckillPrice());
                                seckillOrderTo.setSkuId(skuSeckillInfo.getSkuId());
                                seckillOrderTo.setMemberId(responseVo.getId());
                                seckillOrderTo.setPromotionSessionId(skuSeckillInfo.getPromotionSessionId());
                                seckillOrderTo.setNum(num);
                                rabbitTemplate.convertAndSend("order-event-exchange","order.seckill.order",seckillOrderTo);
                                return timeId;
                            } else {
                                return null;
                            }
                        } else {
                            //不是第一次购买
                            return null;
                        }
                    }
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }
        return null;
    }

    /**
     * 缓存活动信息
     * @param sessions
     */
    private void saveSessionInfos(List<SeckillSessionWithSkus> sessions) {
        sessions.stream().forEach(session->{
            Long start = session.getCreateTime().getTime();
            Long end = session.getEndTime().getTime();
            String key = SESSIONS_CACHE_PREFIX + start + "_" + end;

            if (!redisTemplate.hasKey(key)) {
                //TODO 如果定时任务不加分布式锁，有可能同时在redis添加数据，重复上架
                //有可能这个key还没有放上去另外一个请求也通过了if的判断，但是貌似也没有什么问题无非就是覆盖了
                //如果存在这个key说明已经上架了，就不上架了,没有这个信息才进行上架活动信息
                List<String> skuIds = session.getRelationSkus().stream().map(skuVo -> {
                    return skuVo.getPromotionSessionId().toString() + "_" + skuVo.getSkuId().toString();
                }).collect(Collectors.toList());
                redisTemplate.opsForList().leftPushAll(key,skuIds);

            }
        });

    }

    /**
     * 缓存商品的信息
     * @param sessions
     */
    private void saveSessionSkuInfos(List<SeckillSessionWithSkus> sessions) {
        sessions.stream().forEach(session->{
            //只要执行这个方法了session一定不会是空的
            //收集每一个skuId的消息
            BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
            List<SeckillSkuRelationVo> relationSkus = session.getRelationSkus();
            relationSkus.stream().forEach(skuRelationVo->{
                //这里也绝对不会空指针
                if (!hashOps.hasKey(skuRelationVo.getPromotionSessionId().toString() + "_" + skuRelationVo.getSkuId().toString())){
                    //如果没有当前场次的skuId信息
                    SeckillRelationVoWithSkuInfo seckillRelationVoWithSkuInfo = new SeckillRelationVoWithSkuInfo();
                    //sku基本数据
                    Long skuId = skuRelationVo.getSkuId();
                    R r = productFeignSerivce.getSkuInfo(skuId);
                    if (r.getCode() == 0) {
                        SkuinfoVo skuInfo = (SkuinfoVo) r.getData("skuInfo", new TypeReference<SkuinfoVo>() {});
                        seckillRelationVoWithSkuInfo.setSkuInfo(skuInfo);
                    }
                    //设置商品的秒杀时间信息
                    seckillRelationVoWithSkuInfo.setStart(session.getStartTime().getTime());
                    seckillRelationVoWithSkuInfo.setEnd(session.getEndTime().getTime());
                    //设置商品的随机码，为每一个商品都设置一个随机码，防止恶意攻击
                    String token = UUID.randomUUID().toString().replace("-","");
                    seckillRelationVoWithSkuInfo.setRandomCode(token);
                    RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHONE + token);
                    //将商品可以秒杀的数量作为分布式信号量
                    semaphore.trySetPermits(skuRelationVo.getSeckillCount().intValue());

                    //sku的秒杀信息
                    BeanUtils.copyProperties(skuRelationVo,seckillRelationVoWithSkuInfo);
                    String s = JSON.toJSONString(seckillRelationVoWithSkuInfo);
                    hashOps.put(skuRelationVo.getPromotionSessionId().toString() + "_" + skuRelationVo.getSkuId().toString(),s);
                }

            });
        });
    }

}
