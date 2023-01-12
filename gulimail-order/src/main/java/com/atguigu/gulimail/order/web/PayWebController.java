package com.atguigu.gulimail.order.web;

import com.alipay.api.AlipayApiException;
import com.atguigu.gulimail.order.config.AlipayTemplate;
import com.atguigu.gulimail.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class PayWebController {

    @Autowired
    AlipayTemplate alipayTemplate;
    @Autowired
    OrderService orderService;

    @ResponseBody
    @GetMapping(value = "/aliPayOrder",produces = "text/html")
    public String payOrder(@RequestParam("orderSn") String orderSn) throws AlipayApiException {
        AlipayTemplate.PayVo payVo = orderService.getOrderPay(orderSn);
        String pay = alipayTemplate.pay(payVo);
        return pay;
    }
}
