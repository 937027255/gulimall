package com.atguigu.gulimail.product.service.impl;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimail.product.dao.ProductAttrValueDao;
import com.atguigu.gulimail.product.entity.ProductAttrValueEntity;
import com.atguigu.gulimail.product.service.ProductAttrValueService;


@Service("productAttrValueService")
public class ProductAttrValueServiceImpl extends ServiceImpl<ProductAttrValueDao, ProductAttrValueEntity> implements ProductAttrValueService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<ProductAttrValueEntity> page = this.page(
                new Query<ProductAttrValueEntity>().getPage(params),
                new QueryWrapper<ProductAttrValueEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 根据spuid获取所有的spu属性信息
     * @param spuId
     * @return
     */
    @Override
    public List<ProductAttrValueEntity> listSpu(Long spuId) {
        List<ProductAttrValueEntity> spus = this.list(new QueryWrapper<ProductAttrValueEntity>().eq("spu_id", spuId));
        return spus;
    }

}