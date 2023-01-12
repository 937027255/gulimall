package com.dutir.guilimail.gulimailsearch.controller;

import com.atguigu.common.exception.BizCodeEnum;
import com.atguigu.common.to.es.SkuEsModel;
import com.atguigu.common.utils.R;
import com.dutir.guilimail.gulimailsearch.service.EsProductService;
import com.mysql.cj.log.Log;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping("es/product")
@RestController
@Slf4j
public class EsProductController {

    @Autowired
    private EsProductService esProductService;

    /**
     * 将商品信息保存到es中
     * @param esModels
     * @return
     */
    @PostMapping("save")
    public R saveProduct(@RequestBody List<SkuEsModel> esModels) {
        boolean hasError = true;
        try {
            hasError = esProductService.saveProductInfo(esModels);
        } catch (Exception e) {
            log.error("商品上架错误,{}",e);
            return R.error(BizCodeEnum.PRODDUCT_UP_EXCEPTION.getCode(), BizCodeEnum.PRODDUCT_UP_EXCEPTION.getMsg());
        }
        if (!hasError) return R.ok();
        else return R.error(BizCodeEnum.PRODDUCT_UP_EXCEPTION.getCode(), BizCodeEnum.PRODDUCT_UP_EXCEPTION.getMsg());
    }
}
