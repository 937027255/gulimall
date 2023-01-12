package com.dutir.guilimail.gulimailsearch.controller;

import com.dutir.guilimail.gulimailsearch.service.MallSearchService;
import com.dutir.guilimail.gulimailsearch.vo.SearchParam;
import com.dutir.guilimail.gulimailsearch.vo.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SearchController {

    @Autowired
    MallSearchService mallSearchService;

    /**
     * 传入搜索的参数进行查询
     * @param searchParam
     * @return
     */
    @GetMapping("/list.html")
    public String listPage(SearchParam searchParam, Model model) {
        SearchResult result = mallSearchService.search(searchParam);
        //将结果放回到请求域中
        model.addAttribute("result",result);
        return "list";
    }
}
