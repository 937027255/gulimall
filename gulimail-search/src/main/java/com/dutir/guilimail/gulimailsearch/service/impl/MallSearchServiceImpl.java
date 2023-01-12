package com.dutir.guilimail.gulimailsearch.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.common.to.es.SkuEsModel;
import com.dutir.guilimail.gulimailsearch.config.GulimailElasticSearchConfig;
import com.dutir.guilimail.gulimailsearch.constant.EsConstant;
import com.dutir.guilimail.gulimailsearch.service.MallSearchService;
import com.dutir.guilimail.gulimailsearch.vo.SearchParam;
import com.dutir.guilimail.gulimailsearch.vo.SearchResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class MallSearchServiceImpl implements MallSearchService {

    @Autowired
    RestHighLevelClient client;
    /**
     * 通过传入的检索条件来查询es中的商品
     * @param searchParam
     * @return
     */
    @Override
    public SearchResult search(SearchParam searchParam) {
        SearchResult searchResult = null;
        //构建查询请求
        SearchRequest request = buildSearchRequest(searchParam);
        try {
            //调用es-java-api进行查询，需要传入封装好的request
            SearchResponse searchResponse = client.search(request, GulimailElasticSearchConfig.COMMON_OPTIONS);
            searchResult = parseSearchResult(searchParam,searchResponse);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return searchResult;
    }



    /**
     * 封装es-api请求的方法
     * @param searchParam
     * @return
     */
    private SearchRequest buildSearchRequest(SearchParam searchParam) {
        //创造构建DSL的对象
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //1、构建bool query
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        if (!StringUtils.isEmpty(searchParam.getKeyword())) {
            //按照关键字检索
            boolQueryBuilder.must(QueryBuilders.matchQuery("skuTitle",searchParam.getKeyword()));
        }
        //过滤三级分类的种类
        if (searchParam.getCatalog3Id() != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("catalogId",searchParam.getCatalog3Id()));
        }
        //过滤品牌
        if (searchParam.getBrandId() != null && searchParam.getBrandId().size() > 0) {
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId",searchParam.getBrandId()));
        }
        //过滤是否还有库存
        if (searchParam.getHasStock() != null) {
            boolQueryBuilder.filter(QueryBuilders.termsQuery("hasStock",searchParam.getHasStock() == 1));
        }
        //过滤价格区间_500/500_1000/1000_
        if (!StringUtils.isEmpty(searchParam.getSkuPrice())) {
            String[] prices = searchParam.getSkuPrice().split("_");
            RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("skuPrice");
            if (prices.length == 2) {
                //区间值
                if (!prices[0].isEmpty()) {
                    rangeQueryBuilder.gte(Integer.parseInt(prices[0]));
                }
                rangeQueryBuilder.lte(Integer.parseInt(prices[1]));
            } else if (prices.length == 1) {
                //如果分割后长度为1
                if (searchParam.getSkuPrice().startsWith("_")) {
                    rangeQueryBuilder.lte(Integer.parseInt(prices[0]));
                } else {
                    rangeQueryBuilder.gte(Integer.parseInt(prices[0]));
                }
            }
            boolQueryBuilder.filter(rangeQueryBuilder);
        }
        //根据选择的属性进行检索
        //attrs=1_10.2寸:11寸:12.9寸&2_16G:8G
        List<String> attrs = searchParam.getAttrs();
        if (attrs != null && attrs.size() > 0) {
            for (String attr : attrs) {
                String[] attrSplit = attr.split("_");
                BoolQueryBuilder attrBoolQuery = new BoolQueryBuilder();
                attrBoolQuery.must(QueryBuilders.termQuery("attrs.attrId",attrSplit[0]));
                String[] attrValues = attrSplit[1].split(":");
                attrBoolQuery.must(QueryBuilders.termsQuery("attrs.attrValue",attrValues));
                NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery("attrs", attrBoolQuery, ScoreMode.None);
                boolQueryBuilder.filter(nestedQuery);
            }
        }
        //查询部分封装完毕
        searchSourceBuilder.query(boolQueryBuilder);

        //2、sort sort=saleCount_dest/asc
        if (!StringUtils.isEmpty(searchParam.getSort())) {
            String[] sortSplit = searchParam.getSort().split("_");
            searchSourceBuilder.sort(sortSplit[0],sortSplit[0].equalsIgnoreCase("asc") ? SortOrder.ASC : SortOrder.DESC);
        }
        //3、分页
        Integer pageNum = searchParam.getPageNum();
        searchSourceBuilder.from((pageNum - 1) * EsConstant.PRODUCT_PAGESIZE);
        searchSourceBuilder.size(EsConstant.PRODUCT_PAGESIZE);
        //4、高亮
        if (!StringUtils.isEmpty(searchParam.getKeyword())) {
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field("skuTitle");
            highlightBuilder.preTags("<b stytle='color:red'>");
            highlightBuilder.postTags("</b>");
            searchSourceBuilder.highlighter(highlightBuilder);
        }

        //聚合
        //品牌的聚合
        TermsAggregationBuilder brandAgg = AggregationBuilders.terms("brandAgg").field("brandId").size(10);
        //每一个品牌进行子聚合
        brandAgg.subAggregation(AggregationBuilders.terms("brandNameAgg").field("brandName").size(10));
        brandAgg.subAggregation(AggregationBuilders.terms("brandImgAgg").field("brandImg").size(10));
        searchSourceBuilder.aggregation(brandAgg);

        //种类聚合
        TermsAggregationBuilder catelogAgg = AggregationBuilders.terms("catalogAgg").field("catalogId").size(10);
        catelogAgg.subAggregation(AggregationBuilders.terms("catalogNameAgg").field("catalogName").size(10));
        searchSourceBuilder.aggregation(catelogAgg);

        //attrs聚合
        NestedAggregationBuilder attrsAggsBuilder = AggregationBuilders.nested("attrs", "attrs");

        TermsAggregationBuilder attrIdAgg = AggregationBuilders.terms("attrIdAgg").field("attrs.attrId");
        attrIdAgg.subAggregation(AggregationBuilders.terms("attrNameAgg").field("attrs.attrName"));
        attrIdAgg.subAggregation(AggregationBuilders.terms("attrValueAgg").field("attrs.attrValue"));

        attrsAggsBuilder.subAggregation(attrIdAgg);
        searchSourceBuilder.aggregation(attrsAggsBuilder);

        log.error("构建的DSL语句是:{}",searchSourceBuilder.toString());
        SearchRequest request = new SearchRequest(new String[]{EsConstant.PRODUCT_INDEX},searchSourceBuilder);
        return request;
    }

    /**
     * 解析es-api返回的内容进行vo封装返回
     * @param searchParam
     * @param searchResponse
     * @return
     */
    private SearchResult parseSearchResult(SearchParam searchParam, SearchResponse searchResponse) {
        SearchResult result = new SearchResult();

        SearchHits hits = searchResponse.getHits();

        //1.封装查询到的商品信息
        if (hits.getHits() != null && hits.getHits().length > 0) {
            List<SkuEsModel> skuEsModels = new ArrayList<>();
            for (SearchHit hit : hits) {
                String sourceString = hit.getSourceAsString();
                //转换为java对象
                SkuEsModel esModel = JSON.parseObject(sourceString, SkuEsModel.class);
                skuEsModels.add(esModel);
                //设置高亮属性
                if (!StringUtils.isEmpty(searchParam.getKeyword())) {
                    HighlightField skuTitle = hit.getHighlightFields().get("skuTitle");
                    String highLight = skuTitle.getFragments()[0].toString();
                    esModel.setSkuTitle(highLight);
                }
            }
            result.setProduct(skuEsModels);
        }

        //2.封装分页信息
        //当前页码
        result.setPageNum(searchParam.getPageNum());
        //总记录数
        long total = hits.getTotalHits().value;
        result.setTotal(total);
        //总页码
        int totalPages = (int) (total % EsConstant.PRODUCT_PAGESIZE == 0 ? (int)total / EsConstant.PRODUCT_PAGESIZE : ((int)total / EsConstant.PRODUCT_PAGESIZE + 1));
        result.setTotalPages(totalPages);
        List<Integer> pageNavs = new ArrayList<>();
        for (int i = 1; i < totalPages; i++) {
            pageNavs.add(i);
        }
        result.setPageNavs(pageNavs);
        //3.查询结果设计到的品牌
        List<SearchResult.BrandVo> brandVos = new ArrayList<>();
        Aggregations aggregations = searchResponse.getAggregations();
        ParsedLongTerms brandAgg = aggregations.get("brandAgg");
        for (Terms.Bucket bucket : brandAgg.getBuckets()) {
            //品牌id
            Long brandId = bucket.getKeyAsNumber().longValue();
            //获取子聚合的内容，注意一个品牌id只对应一个图片和品牌名称
            //图片
            ParsedStringTerms brandImgAgg = bucket.getAggregations().get("brandImgAgg");
            String brandImg = brandImgAgg.getBuckets().get(0).getKeyAsString();

            //品牌名
            ParsedStringTerms brandNameAgg = bucket.getAggregations().get("brandNameAgg");
            String brandName = brandNameAgg.getBuckets().get(0).getKeyAsString();

            //封装品牌信息
            SearchResult.BrandVo brandVo = new SearchResult.BrandVo();
            brandVo.setBrandId(brandId);
            brandVo.setBrandName(brandName);
            brandVo.setBrandImg(brandImg);
            brandVos.add(brandVo);
        }
        result.setBrands(brandVos);

        //4.查询涉及到的所有分类
        List<SearchResult.CatalogVo> catalogVos = new ArrayList<>();
        ParsedLongTerms catalogAgg = aggregations.get("catalogAgg");
        for (Terms.Bucket bucket : catalogAgg.getBuckets()) {
            //种类id
            Long catalogId = bucket.getKeyAsNumber().longValue();
            ParsedStringTerms catalogNameAgg = bucket.getAggregations().get("catalogNameAgg");
            String cataLogName = catalogNameAgg.getBuckets().get(0).getKeyAsString();
            SearchResult.CatalogVo catalogVo = new SearchResult.CatalogVo();
            catalogVo.setCatalogId(catalogId);
            catalogVo.setCatalogName(cataLogName);
            catalogVos.add(catalogVo);
        }
        result.setCatalogs(catalogVos);

        //5.查询设计到的所有属性
        List<SearchResult.AttrVo> attrVos = new ArrayList<>();
        ParsedNested attrs = searchResponse.getAggregations().get("attrs");
        ParsedLongTerms attrIdAgg = attrs.getAggregations().get("attrIdAgg");
        for (Terms.Bucket bucket : attrIdAgg.getBuckets()) {
            Long attrId = bucket.getKeyAsNumber().longValue();
            //属性名
            ParsedStringTerms attrNameAgg = bucket.getAggregations().get("attrNameAgg");
            String attrName = attrNameAgg.getBuckets().get(0).getKeyAsString();
            //属性值
            ParsedStringTerms attrValueAgg = bucket.getAggregations().get("attrValueAgg");
            //可以有多个属性值
            List<String> values = new ArrayList<>();
            for (Terms.Bucket attrValueAggBucket : attrValueAgg.getBuckets()) {
                String value = attrValueAggBucket.getKeyAsString();
                values.add(value);
            }
            SearchResult.AttrVo attrVo = new SearchResult.AttrVo();
            attrVo.setAttrId(attrId);
            attrVo.setAttrName(attrName);
            attrVo.setAttrValue(values);
        }
        result.setAttrs(attrVos);

        return result;
    }
}
