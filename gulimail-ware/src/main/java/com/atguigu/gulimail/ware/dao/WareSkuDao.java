package com.atguigu.gulimail.ware.dao;

import com.atguigu.gulimail.ware.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品库存
 * 
 * @author zhangtianyu
 * @email 937027255@qq.com
 * @date 2022-07-08 20:21:11
 */
@Mapper
public interface WareSkuDao extends BaseMapper<WareSkuEntity> {

    void addWare(@Param("skuId") Long skuId, @Param("wareId") Long wareId, @Param("skuNum") Integer skuNum);

    Long getSkuStock(@Param("skuId") Long skuId);

    List<Long> listWareIdHasStock(@Param("skuId") Long skuId);

    int lockStock(@Param("skuId") Long skuId, @Param("wareId") Long wareId, @Param("num") Integer num);

    void unLockStock(@Param("skuId") Long skuId, @Param("wardId") Long wardId, @Param("num") Integer num, @Param("taskDetailId") Long taskDetailId);
}
