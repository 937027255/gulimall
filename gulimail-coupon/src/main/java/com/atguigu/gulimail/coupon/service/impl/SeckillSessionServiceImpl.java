package com.atguigu.gulimail.coupon.service.impl;

import com.atguigu.gulimail.coupon.entity.SeckillSkuRelationEntity;
import com.atguigu.gulimail.coupon.service.SeckillSkuRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimail.coupon.dao.SeckillSessionDao;
import com.atguigu.gulimail.coupon.entity.SeckillSessionEntity;
import com.atguigu.gulimail.coupon.service.SeckillSessionService;


@Service("seckillSessionService")
public class SeckillSessionServiceImpl extends ServiceImpl<SeckillSessionDao, SeckillSessionEntity> implements SeckillSessionService {

    @Autowired
    SeckillSkuRelationService seckillSkuRelationService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SeckillSessionEntity> page = this.page(

                new Query<SeckillSessionEntity>().getPage(params),
                new QueryWrapper<SeckillSessionEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<SeckillSessionEntity> listSeckillOf3Days() {
        //最近三天会上架的秒杀哦活动
        List<SeckillSessionEntity> sessionEntities = baseMapper.selectList(new QueryWrapper<SeckillSessionEntity>().between("start_time", getStartTime(), getEndTime()));
        if (sessionEntities != null && sessionEntities.size() > 0) {
            //通过session找到对应的skuId
            List<SeckillSessionEntity> sessionList = sessionEntities.stream().map(session -> {
                Long id = session.getId();
                //一个活动对应多个skuId一对多的表
                List<SeckillSkuRelationEntity> relationSkus = seckillSkuRelationService.list(new QueryWrapper<SeckillSkuRelationEntity>().eq("promotion_session_id", id));
                session.setRelationSkus(relationSkus);
                return session;
            }).collect(Collectors.toList());
            return sessionList;
        }
        return null;
    }

    public String getStartTime() {
        LocalDate now = LocalDate.now();
        LocalTime min = LocalTime.MIN;
        LocalDateTime dateTime = LocalDateTime.of(now, min);
        String start = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return start;
    }

    public String getEndTime() {
        LocalDate now = LocalDate.now();
        LocalDate localDate = now.plusDays(2);
        LocalTime max = LocalTime.MAX;
        LocalDateTime dateTime = LocalDateTime.of(localDate, max);
        String end = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return end;
    }

}