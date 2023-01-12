package com.atguigu.gulimail.product.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.consts.SpuStatusConst;
import com.atguigu.common.to.*;
import com.atguigu.common.to.es.SkuEsModel;
import com.atguigu.common.utils.R;
import com.atguigu.gulimail.product.entity.*;
import com.atguigu.gulimail.product.feign.CouponFeignService;
import com.atguigu.gulimail.product.feign.SearchFeignService;
import com.atguigu.gulimail.product.feign.WareFeignService;
import com.atguigu.gulimail.product.service.*;
import com.atguigu.gulimail.product.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimail.product.dao.SpuInfoDao;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("spuInfoService")
@Slf4j
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {

    @Autowired
    SpuInfoDescService spuInfoDescService;
    @Autowired
    SpuImagesService spuImagesService;
    @Autowired
    AttrService attrService;
    @Autowired
    ProductAttrValueService productAttrValueService;
    @Autowired
    SkuInfoService skuInfoService;
    @Autowired
    SkuImagesService skuImagesService;
    @Autowired
    SkuSaleAttrValueService skuSaleAttrValueService;
    @Autowired
    CouponFeignService couponFeignService;
    @Autowired
    BrandService brandService;
    @Autowired
    CategoryService categoryService;
    @Autowired
    WareFeignService wareFeignService;
    @Autowired
    SearchFeignService searchFeignService;


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                new QueryWrapper<SpuInfoEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 保存spu的信息
     * @param spuSaveVo
     */
    //TODO 分布式事务等内容在高级篇中讲解
    @Transactional
    @Override
    public void saveSpuInfo(SpuSaveVo spuSaveVo) {
        //1、保存spu的基本信息
        SpuInfoEntity spuInfoEntity = new SpuInfoEntity();
        BeanUtils.copyProperties(spuSaveVo,spuInfoEntity);
        baseMapper.insert(spuInfoEntity);
        //2、保存spu的描述图片
        List<String> decripts = spuSaveVo.getDecript();
        SpuInfoDescEntity desc = new SpuInfoDescEntity();
        desc.setSpuId(spuInfoEntity.getId());
        desc.setDecript(String.join(",",decripts));
        spuInfoDescService.save(desc);
        //3、保存spu的图片集
        List<String> images = spuSaveVo.getImages();
        if (images != null || images.size() > 0) {
            List<SpuImagesEntity> collectImages = images.stream().map(m -> {
                SpuImagesEntity spuImagesEntity = new SpuImagesEntity();
                spuImagesEntity.setSpuId(spuInfoEntity.getId());
                spuImagesEntity.setImgUrl(m);
                return spuImagesEntity;
            }).collect(Collectors.toList());
            spuImagesService.saveBatch(collectImages);
        }

        //4、保存spu的规格参数
        List<BaseAttrs> baseAttrs = spuSaveVo.getBaseAttrs();
        //封装到实体类中去
        List<ProductAttrValueEntity> collectAttrs = baseAttrs.stream().map(attr -> {
            ProductAttrValueEntity attrValueEntity = new ProductAttrValueEntity();
            attrValueEntity.setAttrId(attr.getAttrId());
            attrValueEntity.setAttrValue(attr.getAttrValues());
            attrValueEntity.setQuickShow(attr.getShowDesc());
            attrValueEntity.setSpuId(spuInfoEntity.getId());
            AttrEntity attrEntity = attrService.getById(attr.getAttrId());
            attrValueEntity.setAttrName(attrEntity.getAttrName());
            return attrValueEntity;
        }).collect(Collectors.toList());
        productAttrValueService.saveBatch(collectAttrs);
        //6、保存spu的积分信息
        SpuBoundTo spuBoundTo = new SpuBoundTo();
        spuBoundTo.setBuyBounds(spuSaveVo.getBounds().getBuyBounds());
        spuBoundTo.setGrowBounds(spuSaveVo.getBounds().getGrowBounds());
        spuBoundTo.setSpuId(spuInfoEntity.getId());
        //发起远程调用
        R r = couponFeignService.saveBounds(spuBoundTo);
        if (r.getCode() != 0) {
            log.error("远程保存spu优惠信息失败");
        }

        //5、保存spu的所有sku信息
            //5.1、sku的基本信息
        List<Skus> skus = spuSaveVo.getSkus();
        if (skus != null && skus.size() >0) {
            skus.forEach(sku ->{
                String defaultImg = "";
                for (Images image : sku.getImages()) {
                    if (image.getDefaultImg() == 1) {
                        defaultImg = image.getImgUrl();
                    }
                }
                SkuInfoEntity skuInfoEntity = new SkuInfoEntity();
                skuInfoEntity.setSpuId(spuInfoEntity.getId());
                BeanUtils.copyProperties(sku,skuInfoEntity);
                skuInfoEntity.setBrandId(spuInfoEntity.getBrandId());
                skuInfoEntity.setCatalogId(spuInfoEntity.getCatalogId());
                skuInfoEntity.setSaleCount(0L);
                skuInfoEntity.setSkuDefaultImg(defaultImg);
                //把这一个skuInfo信息保存到数据库中
                skuInfoService.save(skuInfoEntity);
                //5.2、sku的图片信息
                List<SkuImagesEntity> skuImgs = sku.getImages().stream().map(img -> {
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    skuImagesEntity.setSkuId(skuImagesEntity.getSkuId());
                    skuImagesEntity.setImgUrl(img.getImgUrl());
                    skuImagesEntity.setDefaultImg(img.getDefaultImg());
                    return skuImagesEntity;
                }).filter(entity->{
                    return !StringUtils.isEmpty(entity.getImgUrl());
                }).collect(Collectors.toList());
                //将skuimage保存到数据库中
                skuImagesService.saveBatch(skuImgs);
                //5.3、sku的销售属性值
                List<SkuSaleAttrValueEntity> skuAttrList = sku.getAttr().stream().map(skuAttr -> {
                    SkuSaleAttrValueEntity skuSaleAttrValueEntity = new SkuSaleAttrValueEntity();
                    skuSaleAttrValueEntity.setSkuId(skuInfoEntity.getSkuId());
                    skuSaleAttrValueEntity.setAttrId(skuAttr.getAttrId());
                    AttrEntity attr = attrService.getById(skuAttr.getAttrId());
                    skuSaleAttrValueEntity.setAttrName(attr.getAttrName());
                    skuSaleAttrValueEntity.setAttrValue(skuAttr.getAttrValue());
                    return skuSaleAttrValueEntity;
                }).collect(Collectors.toList());
                skuSaleAttrValueService.saveBatch(skuAttrList);
                //5.4、sku的优惠和满减信息
                SkuLadderTo skuLadderTo = new SkuLadderTo();
                skuLadderTo.setSkuId(skuInfoEntity.getSkuId());
                skuLadderTo.setPrice(sku.getPrice());
                skuLadderTo.setDiscount(sku.getDiscount());
                skuLadderTo.setFullCount(sku.getFullCount());
                skuLadderTo.setAddOther(sku.getCountStatus());
                if (skuLadderTo.getFullCount().compareTo(new BigDecimal("0")) == 1)
                    couponFeignService.saveLadder(skuLadderTo);

                SkuReductionTo skuReductionTo = new SkuReductionTo();
                skuReductionTo.setSkuId(skuInfoEntity.getSkuId());
                skuReductionTo.setFullPrice(sku.getFullPrice());
                skuReductionTo.setReducePrice(sku.getReducePrice());
                skuReductionTo.setAddOther(sku.getPriceStatus());
                if (skuReductionTo.getFullPrice().compareTo(new BigDecimal("0")) == 1)
                    couponFeignService.saveReduction(skuReductionTo);

                //保存会员价格
                MemberPriceTo memberPriceTo = new MemberPriceTo();
                BeanUtils.copyProperties(sku,memberPriceTo);
                memberPriceTo.setSkuId(skuInfoEntity.getSkuId());
                couponFeignService.saveMemberPrice(memberPriceTo);
            });
        }
    }

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {

        QueryWrapper<SpuInfoEntity> wrapper = new QueryWrapper<>();

        String key = (String) params.get("key");
        String catelogId = (String) params.get("catelogId");
        String brandId = (String) params.get("brandId");
        String status = (String) params.get("status");

        if (!StringUtils.isEmpty(key) && !"0".equals(catelogId)) {
            wrapper.and(w->{
                w.eq("id",key).or().like("spu_name",key);
            });
        }
        if (!StringUtils.isEmpty(catelogId)) {
            wrapper.eq("catalog_id",catelogId);
        }
        if (!StringUtils.isEmpty(brandId) && !brandId.equals("0")) {
            wrapper.eq("brand_id",brandId);
        }
        if (!StringUtils.isEmpty(status)) {
            wrapper.eq("publish_status",status);
        }

        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    @Override
    public void spuUp(Long spuId) {
        //1、查出当前spuid对应的所有sku信息，品牌的名字
        List<SkuInfoEntity> skus = skuInfoService.getSkusBySpuId(spuId);
        //2、封装每一个sku的信息

        //TODO 4、查询当前sku的所有可以被用来检索的规格属性
        List<ProductAttrValueEntity> productAttrValueEntities = productAttrValueService.listSpu(spuId);
        //收集返回数据中的attrid用于查询所有的能够被用来检索的规格属性
        List<Long> attrIds = productAttrValueEntities.stream().map(attrVal -> {
            return attrVal.getAttrId();
        }).collect(Collectors.toList());
        //查询出能作为检索的规格
        List<AttrEntity> attrs = attrService.getAttrsByIdsAndSearchCondition(attrIds);
        List<Long> searchIds = attrs.stream().map(attr -> {
            return attr.getAttrId();
        }).collect(Collectors.toList());
        HashSet<Long> idSet = new HashSet<>(searchIds);
        List<SkuEsModel.Attrs> searchAttrs = productAttrValueEntities.stream().filter(item -> {
            return idSet.contains(item.getAttrId());
        }).map(item -> {
            SkuEsModel.Attrs esAttrs = new SkuEsModel.Attrs();
            BeanUtils.copyProperties(item, esAttrs);
            return esAttrs;
        }).collect(Collectors.toList());

        //统一查询，库存系统中是否还有库存
        List<Long> skuIds = skus.stream().map(sku -> {
            return sku.getSkuId();
        }).collect(Collectors.toList());
        Map<Long, Boolean> stockMap = null;
        try {
            R<List<SkuStockVo>> skuHasStock = wareFeignService.getSkuHasStock(skuIds);
            TypeReference<List<SkuStockVo>> listTypeReference = new TypeReference<List<SkuStockVo>>(){};
            stockMap = skuHasStock.getData(listTypeReference).stream().collect(Collectors.toMap(stockVo -> stockVo.getSkuId(), stockVo -> stockVo.getHasStock()));

        } catch (Exception e) {
            log.error("库存服务查询异常:原因{}",e);
        }

        Map<Long, Boolean> finalStockMap = stockMap;
        List<SkuEsModel> esModels = skus.stream().map(sku -> {
            SkuEsModel skuEsModel = new SkuEsModel();
            BeanUtils.copyProperties(sku, skuEsModel);
            skuEsModel.setSkuPrice(sku.getPrice());
            skuEsModel.setSkuImg(sku.getSkuDefaultImg());
            //hasStock、hotScore
            //TODO 1、发送远程调用，库存系统查询是否有库存
            if (finalStockMap == null) {
                skuEsModel.setHasStock(true);
            } else {
                skuEsModel.setHasStock(finalStockMap.get(sku.getSkuId()));
            }

            //TODO 2、热度评分，暂时赋值为0
            skuEsModel.setHotScore(0L);

            //TODO 3、 查询品牌和分类的名字信息
            BrandEntity brand = brandService.getById(skuEsModel.getBrandId());
            skuEsModel.setBrandName(brand.getName());
            skuEsModel.setBrandImg(brand.getLogo());
            CategoryEntity category = categoryService.getById(skuEsModel.getCatalogId());
            skuEsModel.setCatalogName(category.getName());
            //4、查询当前sku的所有可以被用来检索的规格属性
            skuEsModel.setAttrs(searchAttrs);

            return skuEsModel;
        }).collect(Collectors.toList());

        //TODO 5、将数据发送给es进行保存
        R r = searchFeignService.saveProduct(esModels);
        System.out.println(r.getCode());
        if (r.getCode() == 0) {
            //远程调用成功
            //TODO 6、修改上架状态
            baseMapper.updateSpuStatus(spuId, SpuStatusConst.SPU_UP.getCode());
        } else {
            //远程调用失败
            //TODO 7、考虑接口幂等性的问题，重复调用？以后讨论
        }

    }

    @Override
    public SpuInfoEntity getSpuInfoBySkuId(Long skuId) {
        //查询spuId
        SkuInfoEntity skuInfo = skuInfoService.getById(skuId);
        Long spuId = skuInfo.getSpuId();
        SpuInfoEntity spuInfoEntity = this.getById(spuId);
        return spuInfoEntity;
    }

}