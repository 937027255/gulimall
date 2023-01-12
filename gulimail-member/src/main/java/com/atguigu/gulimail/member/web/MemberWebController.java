package com.atguigu.gulimail.member.web;

import com.atguigu.common.utils.R;
import com.atguigu.gulimail.member.feign.OrderFeignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.Map;

@Controller
public class MemberWebController {

    @Autowired
    OrderFeignService orderFeignService;

    @GetMapping("/memberOrder.html")
    public String getMemberOrderPage(@RequestParam(value = "pageNum",defaultValue = "1") Integer pageNum, Model model) {
        //查询出当单登陆的所有订单数据
        HashMap<String, Object> map = new HashMap<>();
        map.put("page",pageNum.toString());
        R r = orderFeignService.listWithItem(map);
        model.addAttribute("orders",r);
        return "orderList";
    }
}
