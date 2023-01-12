package com.atguigu.gulimail.product.vo.front;

import com.atguigu.gulimail.product.entity.SkuImagesEntity;
import com.atguigu.gulimail.product.entity.SkuInfoEntity;
import com.atguigu.gulimail.product.entity.SpuInfoDescEntity;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
public class SkuItemVo {

    /*** 1 sku基本信息的获取:如标题*/
    SkuInfoEntity info;

    boolean hasStock = true;

    /*** 2 sku的图片信息*/
    List<SkuImagesEntity> images;

    /*** 3 获取spu的销售属性组合。每个attrName对应一个value-list*/
    List<ItemSaleAttrVo> saleAttr;

    /*** 4 获取spu的介绍*/
    SpuInfoDescEntity desc;

    /*** 5 获取spu的规格参数信息，每个分组的包含list*/
    List<SpuItemAttrGroup> groupAttrs;

    /*** 6 秒杀信息*/
    SeckillSkuInfo seckillInfoVo;

    @ToString
    @Data
    public static class ItemSaleAttrVo {
        private Long attrId;
        private String attrName;

        /** AttrValueWithSkuIdVo两个属性 attrValue、skuIds */
        private List<AttrValueWithSkuIdVo> attrValues;
    }

    @ToString
    @Data
    public static class SpuItemAttrGroup {
        private String groupName;

        /** 两个属性attrName、attrValue */
        //记录一个组下的所有属性名和属性值
        private List<SpuBaseAttrVo> attrs;
    }

    @ToString
    @Data
    public static class SpuBaseAttrVo {
        private String attrName;
        private String attrValue;
    }
}





