package com.dutir.guilimail.gulimailsearch.vo;

import lombok.Data;

import java.util.List;

@Data
public class SearchParam {
    private String keyword;//页面传递过来的关键字
    private Long catalog3Id;//按照三级分类传递过来的分类id

    private String sort;//排序条件

    private Integer hasStock;//是否只显示有货
    private String skuPrice;//按照价格区间进行显示
    private List<Long> brandId;//按照品牌进行查询，可以多选
    private List<String> attrs;//按照商品属性进行查询，可以多选
    private Integer pageNum = 1;//页码
}
