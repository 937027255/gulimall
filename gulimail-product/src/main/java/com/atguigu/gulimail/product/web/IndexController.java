package com.atguigu.gulimail.product.web;

import com.atguigu.gulimail.product.entity.CategoryEntity;
import com.atguigu.gulimail.product.service.CategoryService;
import com.atguigu.gulimail.product.vo.front.Catelog2Vo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;


@Controller
public class IndexController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping({"/","index.html"})
    public String getIndexPage(Model model) {
        List<CategoryEntity> catagories = categoryService.getLevelOneCatagories();
        //会将信息放入到请求域中去
        model.addAttribute("categorys",catagories);
        //进行路径跳转
        return "index";
    }
    @ResponseBody
    @GetMapping("/index/catalog.json")
    public Map<String,List<Catelog2Vo>> getCatalogJson() {
        Map<String,List<Catelog2Vo>> map = categoryService.getCatalogJson();
        return map;
    }

}

