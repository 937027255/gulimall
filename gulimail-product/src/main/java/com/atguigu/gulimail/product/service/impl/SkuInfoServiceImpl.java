package com.atguigu.gulimail.product.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.R;
import com.atguigu.gulimail.product.entity.SkuImagesEntity;
import com.atguigu.gulimail.product.entity.SpuInfoDescEntity;
import com.atguigu.gulimail.product.feign.SeckillFeignSerive;
import com.atguigu.gulimail.product.service.*;
import com.atguigu.gulimail.product.vo.front.SeckillSkuInfo;
import com.atguigu.gulimail.product.vo.front.SkuItemVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimail.product.dao.SkuInfoDao;
import com.atguigu.gulimail.product.entity.SkuInfoEntity;
import org.springframework.util.ResizableByteArrayOutputStream;
import org.springframework.util.StringUtils;

import javax.sound.sampled.Line;


@Service("skuInfoService")
public class SkuInfoServiceImpl extends ServiceImpl<SkuInfoDao, SkuInfoEntity> implements SkuInfoService {

    @Autowired
    SkuImagesService skuImagesService;
    @Autowired
    SpuInfoDescService spuInfoDescService;
    @Autowired
    AttrGroupService attrGroupService;
    @Autowired
    SkuSaleAttrValueService skuSaleAttrValueService;
    @Autowired
    ThreadPoolExecutor executor;
    @Autowired
    SeckillFeignSerive seckillFeignSerive;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                new QueryWrapper<SkuInfoEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        QueryWrapper<SkuInfoEntity> wrapper = new QueryWrapper<>();
        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)) {
            wrapper.and(w->{
                w.eq("sku_id",key).or().like("sku_name",key);
            });
        }
        String catelogId = (String) params.get("catelogId");
        String brandId = (String) params.get("brandId");
        if (!StringUtils.isEmpty(catelogId) && !"0".equals(catelogId)) {
            wrapper.eq("catalog_id",catelogId);
        }
        if (!StringUtils.isEmpty(brandId) && !brandId.equals("0")) {
            wrapper.eq("brand_id",brandId);
        }
        String min = (String) params.get("min");
        String max = (String) params.get("max");
        if (!StringUtils.isEmpty(min)) {
            wrapper.ge("price",min);
        }
        if (!StringUtils.isEmpty(max) && !"0".equals(max)) {
            try {
                BigDecimal bigDecimal = new BigDecimal(max);
                if (bigDecimal.compareTo(new BigDecimal("0")) == 1)
                    wrapper.le("price",max);
            }catch (Exception e) {
                e.printStackTrace();
            }

        }


        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    @Override
    public List<SkuInfoEntity> getSkusBySpuId(Long spuId) {
        List<SkuInfoEntity> list = this.list(new QueryWrapper<SkuInfoEntity>().eq("spu_id", spuId));
        return list;
    }

    @Override
    public SkuItemVo skuItem(Long skuId) throws ExecutionException, InterruptedException {
        SkuItemVo itemVo = new SkuItemVo();
        CompletableFuture<SkuInfoEntity> infoFuture = CompletableFuture.supplyAsync(() -> {
            //1、sku基本信息获取
            SkuInfoEntity skuinfo = this.getById(skuId);
            itemVo.setInfo(skuinfo);
            return skuinfo;
        });
//        Long spuId = skuinfo.getSpuId();
//        Long catalogId = skuinfo.getCatalogId();
        CompletableFuture<Void> imagesFuture = CompletableFuture.runAsync(() -> {
            //2、sku图片信息
            List<SkuImagesEntity> skuImagesEntities = skuImagesService.getImagesBySkuId(skuId);
            itemVo.setImages(skuImagesEntities);
        },executor);
        CompletableFuture<Void> saleAttrFuture = infoFuture.thenAcceptAsync((res) -> {
                //3、spu的销售属性组合
                List<SkuItemVo.ItemSaleAttrVo> skuSaleAttrVos = skuSaleAttrValueService.getSaleAttrBySpuId(res.getSpuId());
                itemVo.setSaleAttr(skuSaleAttrVos);
        },executor);
        CompletableFuture<Void> descFuture = infoFuture.thenAcceptAsync((res) -> {
            //4、spu的介绍
            SpuInfoDescEntity spuInfoDesc = spuInfoDescService.getById(res.getSpuId());
            itemVo.setDesc(spuInfoDesc);
        }, executor);
        CompletableFuture<Void> spuAttrFuture = infoFuture.thenAcceptAsync((res) -> {
            //5、获取spu的规格参数信息,需要通过一个spu找到所有的参数信息
            List<SkuItemVo.SpuItemAttrGroup> attrGroupVos = attrGroupService.getAttrGroupWithAttrsBySpuId(res.getSpuId(), res.getCatalogId());
            itemVo.setGroupAttrs(attrGroupVos);
        }, executor);
        //TODO 6、秒杀商品的优惠信息
        CompletableFuture<Void> seckillFuture = CompletableFuture.runAsync(() -> {
            R r = seckillFeignSerive.getSeckillSkuById(skuId);
            if (r.getCode() == 0) {
                SeckillSkuInfo seckillSkuInfo = (SeckillSkuInfo) r.getData(new TypeReference<SeckillSkuInfo>() {
                });
                itemVo.setSeckillInfoVo(seckillSkuInfo);
            }
        }, executor);

        CompletableFuture.allOf(imagesFuture,saleAttrFuture,descFuture,spuAttrFuture,seckillFuture).get();

        return itemVo;
    }

}