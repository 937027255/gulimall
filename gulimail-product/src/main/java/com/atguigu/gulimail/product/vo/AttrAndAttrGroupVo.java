package com.atguigu.gulimail.product.vo;

import com.atguigu.gulimail.product.entity.AttrEntity;
import lombok.Data;

import java.util.List;

@Data
public class AttrAndAttrGroupVo {
    private Long attrGroupId;

    private String attrGroupName;

    private Integer sort;

    private String descript;

    private String icon;

    private Long catelogId;

    List<AttrEntity> attrs;
}
