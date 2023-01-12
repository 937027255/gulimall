package com.dutir.guilimail.gulimailsearch.service;

import com.atguigu.common.to.es.SkuEsModel;

import java.io.IOException;
import java.util.List;

public interface EsProductService {
    boolean saveProductInfo(List<SkuEsModel> esModels) throws IOException;
}
