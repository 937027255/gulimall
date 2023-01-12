package com.atguigu.gulimail.coupon.service.impl;

import com.atguigu.common.to.MemberPrice;
import com.atguigu.common.to.MemberPriceTo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimail.coupon.dao.MemberPriceDao;
import com.atguigu.gulimail.coupon.entity.MemberPriceEntity;
import com.atguigu.gulimail.coupon.service.MemberPriceService;


@Service("memberPriceService")
public class MemberPriceServiceImpl extends ServiceImpl<MemberPriceDao, MemberPriceEntity> implements MemberPriceService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberPriceEntity> page = this.page(
                new Query<MemberPriceEntity>().getPage(params),
                new QueryWrapper<MemberPriceEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void saveInfo(MemberPriceTo memberPriceTo) {
        List<MemberPrice> memberPrice = memberPriceTo.getMemberPrice();
        List<MemberPriceEntity> collect = memberPrice.stream().map(mp -> {
            MemberPriceEntity memberPriceEntity = new MemberPriceEntity();
            memberPriceEntity.setSkuId(memberPriceTo.getSkuId());
            memberPriceEntity.setMemberLevelName(mp.getName());
            memberPriceEntity.setMemberPrice(mp.getPrice());
            memberPriceEntity.setMemberLevelId(mp.getId());
            memberPriceEntity.setAddOther(1);
            return memberPriceEntity;
        }).filter(item->{
            return item.getMemberPrice().compareTo(new BigDecimal("0")) == 1;
        }).collect(Collectors.toList());
        this.saveBatch(collect);
    }


}