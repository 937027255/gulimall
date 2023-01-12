package com.atguigu.gulimail.product.service;

import com.atguigu.gulimail.product.vo.front.Catelog2Vo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimail.product.entity.CategoryEntity;

import java.util.List;
import java.util.Map;

/**
 * ??Ʒ???????
 *
 * @author zhangtianyu
 * @email 937027255@qq.com
 * @date 2022-07-08 14:31:00
 */
public interface CategoryService extends IService<CategoryEntity> {

    PageUtils queryPage(Map<String, Object> params);

    List<CategoryEntity> showCategoryWithTree();


    void removeMenusByIds(List<Long> asList);

    void updateDetail(CategoryEntity category);

    List<CategoryEntity> getLevelOneCatagories();

    Map<String, List<Catelog2Vo>> getCatalogJson();

}

