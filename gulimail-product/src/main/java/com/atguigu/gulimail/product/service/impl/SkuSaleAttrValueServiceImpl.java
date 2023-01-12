package com.atguigu.gulimail.product.service.impl;

import com.atguigu.gulimail.product.vo.front.SkuItemVo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimail.product.dao.SkuSaleAttrValueDao;
import com.atguigu.gulimail.product.entity.SkuSaleAttrValueEntity;
import com.atguigu.gulimail.product.service.SkuSaleAttrValueService;


@Service("skuSaleAttrValueService")
public class SkuSaleAttrValueServiceImpl extends ServiceImpl<SkuSaleAttrValueDao, SkuSaleAttrValueEntity> implements SkuSaleAttrValueService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuSaleAttrValueEntity> page = this.page(
                new Query<SkuSaleAttrValueEntity>().getPage(params),
                new QueryWrapper<SkuSaleAttrValueEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<SkuItemVo.ItemSaleAttrVo> getSaleAttrBySpuId(Long spuId) {
        List<SkuItemVo.ItemSaleAttrVo> saleAttrVos = baseMapper.getSaleAttrBySpuId(spuId);
        return saleAttrVos;
    }


    @Override
    public List<String> getSaleAttrList(Long skuId) {
        List<String> saleAttrList = baseMapper.getSaleAttrList(skuId);
        return saleAttrList;
    }

}