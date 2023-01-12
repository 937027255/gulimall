package com.atguigu.gulimail.product.web;

import com.atguigu.gulimail.product.service.SkuInfoService;
import com.atguigu.gulimail.product.vo.front.SkuItemVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.concurrent.ExecutionException;

@Controller
public class ItemController {

    @Autowired
    SkuInfoService skuInfoService;

    @GetMapping("/{skuId}.html")
    public String skuItem(@PathVariable Long skuId, Model model) throws ExecutionException, InterruptedException {
        SkuItemVo itemVo = skuInfoService.skuItem(skuId);
        model.addAttribute("item",itemVo);
        System.out.println(itemVo);
        return "item";
    }
}
