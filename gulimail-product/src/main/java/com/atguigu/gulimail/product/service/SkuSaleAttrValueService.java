package com.atguigu.gulimail.product.service;

import com.atguigu.gulimail.product.vo.front.SkuItemVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimail.product.entity.SkuSaleAttrValueEntity;

import java.util.List;
import java.util.Map;

/**
 * sku????????&ֵ
 *
 * @author zhangtianyu
 * @email 937027255@qq.com
 * @date 2022-07-08 14:31:00
 */
public interface SkuSaleAttrValueService extends IService<SkuSaleAttrValueEntity> {

    PageUtils queryPage(Map<String, Object> params);

    List<SkuItemVo.ItemSaleAttrVo> getSaleAttrBySpuId(Long spuId);

    List<String> getSaleAttrList(Long skuId);
}

