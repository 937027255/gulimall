package com.dutir.guilimail.gulimailsearch.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.common.to.es.SkuEsModel;
import com.dutir.guilimail.gulimailsearch.config.GulimailElasticSearchConfig;
import com.dutir.guilimail.gulimailsearch.constant.EsConstant;
import com.dutir.guilimail.gulimailsearch.service.EsProductService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class EsProductServiceImpl implements EsProductService {

    @Autowired
    RestHighLevelClient client;


    @Override
    public boolean saveProductInfo(List<SkuEsModel> esModels) throws IOException {
        BulkRequest bulkRequest = new BulkRequest();
        for(SkuEsModel esModel : esModels) {
            IndexRequest indexRequest = new IndexRequest(EsConstant.PRODUCT_INDEX);
            indexRequest.id(esModel.getSkuId().toString());
            String s = JSON.toJSONString(esModel);
            indexRequest.source(s, XContentType.JSON);
            bulkRequest.add(indexRequest);
        }
        BulkResponse bulk = client.bulk(bulkRequest, GulimailElasticSearchConfig.COMMON_OPTIONS);
        //TODO 如果批量处理错误
        boolean res = bulk.hasFailures();
        List<String> collect = Arrays.stream(bulk.getItems()).map(item -> {
            return item.getId();
        }).collect(Collectors.toList());
        log.info("商品上架成功:{}",collect);
        return res;
    }
}
