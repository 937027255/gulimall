package com.atguigu.gulimail.product.service;

import com.atguigu.gulimail.product.vo.AttrRespVo;
import com.atguigu.gulimail.product.vo.AttrVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimail.product.entity.AttrEntity;

import java.util.List;
import java.util.Map;

/**
 * ??Ʒ?
 *
 * @author zhangtianyu
 * @email 937027255@qq.com
 * @date 2022-07-08 14:31:00
 */
public interface AttrService extends IService<AttrEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveAttr(AttrVo attr);

    PageUtils getBaseList(Map<String, Object> params, Long catelogId, String type);

    AttrRespVo getAttrDetailById(Long attrId);

    void updateAttrDetail(AttrVo attr);

    /**
     * 根绝attrids和检索条件查询符合条件的规格属性
     * @param attrIds
     * @return
     */
    List<AttrEntity> getAttrsByIdsAndSearchCondition(List<Long> attrIds);
}

