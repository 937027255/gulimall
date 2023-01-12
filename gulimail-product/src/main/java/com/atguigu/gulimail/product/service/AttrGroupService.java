package com.atguigu.gulimail.product.service;

import com.atguigu.gulimail.product.entity.AttrEntity;
import com.atguigu.gulimail.product.vo.AttrAndAttrGroupVo;
import com.atguigu.gulimail.product.vo.AttrGroupRelationVo;
import com.atguigu.gulimail.product.vo.AttrRespVo;
import com.atguigu.gulimail.product.vo.front.SkuItemVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimail.product.entity.AttrGroupEntity;

import java.util.List;
import java.util.Map;

/**
 * ???Է??
 *
 * @author zhangtianyu
 * @email 937027255@qq.com
 * @date 2022-07-08 14:31:00
 */
public interface AttrGroupService extends IService<AttrGroupEntity> {

    PageUtils queryPage(Map<String, Object> params);

    PageUtils queryPageByCateId(Map<String, Object> params, Long catelogId);

    Long[] findCatelogPath(Long catelogId);

    List<AttrEntity> getAttrRelation(Long attrgroupId);

    void deleteAttrGroupRelation(AttrGroupRelationVo[] vos);

    PageUtils getNoRelationGroup(Map<String, Object> params, Long attrgroupId);

    List<AttrAndAttrGroupVo> getAttrAndGroupByCatId(Long catelogId);

    List<SkuItemVo.SpuItemAttrGroup> getAttrGroupWithAttrsBySpuId(Long spuId, Long catalogId);
}

