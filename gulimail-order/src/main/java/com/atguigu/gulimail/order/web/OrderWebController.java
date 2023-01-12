package com.atguigu.gulimail.order.web;

import com.atguigu.gulimail.order.service.OrderService;

import com.atguigu.gulimail.order.vo.OrderConfirmVo;
import com.atguigu.gulimail.order.vo.OrderSubmitVo;
import com.atguigu.gulimail.order.vo.SubmitOrderResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.concurrent.ExecutionException;

@Controller
public class OrderWebController {

    @Autowired
    OrderService orderService;


    /**
     * 点击去结算页面跳转到的逻辑
     * @param model
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @GetMapping("toTrade")
    public String toTrade(Model model) throws ExecutionException, InterruptedException {
        //需要获取vo的结果
        OrderConfirmVo confirmVo = orderService.confirmOrder();
        model.addAttribute("confirmOrderData",confirmVo);
        return "confirm";
    }

    /**
     * 点击提交订单进行验证的逻辑
     * @param
     * @return
     */
    @PostMapping("/submitOrder")
    public String submitOrder(OrderSubmitVo orderSubmitVo, Model model, RedirectAttributes redirectAttributes) {
        //检验下单是否能够成功

        SubmitOrderResponseVo submitOrderResponseVo = orderService.submitOrder(orderSubmitVo);
        //下单成功跳转到支付选择页面
        if (submitOrderResponseVo.getCode() == 0) {
            //下单成功
            //下单：去创建订单，验证令牌，验证价格，锁定库存
            model.addAttribute("order",submitOrderResponseVo.getOrder());
            return "pay";
        } else {
            //下单失败回到订单页面重新确认订单信息
            String msg = "下单失败：";
            switch (submitOrderResponseVo.getCode()) {
                case 1:
                    msg += "订单信息过期，请重新提交";
                    break;
                case 2:
                    msg += "订单商品价格发生变化，请确认后再次提交";
                    break;
                case 3:
                    msg += "商品库存不足";
            }
            redirectAttributes.addFlashAttribute("msg",msg);
            return "redirect:http://order.gulimail.com/toTrade";
        }
    }
}
