package com.dutir.guilimail.cart.controller;

import com.dutir.guilimail.cart.constant.CartConstant;
import com.dutir.guilimail.cart.interceptor.GulimailInterceptor;
import com.dutir.guilimail.cart.service.CartService;
import com.dutir.guilimail.cart.vo.CartItemVo;
import com.dutir.guilimail.cart.vo.CartVo;
import com.dutir.guilimail.cart.vo.LoginConfirmVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Controller
public class CartController {

    @Autowired
    CartService cartService;

    /**
     * 获取所有的购物车信息列表并返回展示页面
     * @param
     * @param model
     * @return
     */
    @GetMapping("cart.html")
    public String cartListPage(HttpServletRequest request, Model model) throws ExecutionException, InterruptedException {

        String value = null;
        for (Cookie cookie : request.getCookies()) {
            if (cookie.getName().equals(CartConstant.TEMP_USER_COOKIE_NAME)) {
                value = cookie.getValue();
            }
        }
        CartVo cartVo = cartService.getCarts(value);
        model.addAttribute("cart",cartVo);
        return "cartList";
    }

    /**
     * 添加商品到购物车
     * @return
     */
    @GetMapping("/addToCart")
    public String addToCart(@RequestParam("skuId") Long skuId, @RequestParam("num") Integer num,RedirectAttributes redirectAttributes) throws ExecutionException, InterruptedException {
        //返回一条购物车信息
        CartItemVo cartItemVo = cartService.addToCart(skuId,num);
        redirectAttributes.addAttribute("skuId",skuId);
//        model.addAttribute("cartItem",cartItemVo);
        System.out.println("成功加入到购物车...");
        return "redirect:http://cart.gulimail.com/successAddToCartPage.html";
    }

    @GetMapping("successAddToCartPage.html")
    public String successAddToCartPage(@RequestParam("skuId") Long skuId,Model model) {
        /**
         * 通过指定的skuId去redis中找到对应的购物车内容
         */
        CartItemVo cartItemVo = cartService.getCartItem(skuId);
        model.addAttribute("cartItem",cartItemVo);
        return "success";
    }

    /**
     * 修改购物车是否选中
     * @return
     */
    @GetMapping("/checkItem")
    public String checkItem(@RequestParam("skuId") Long skuId,@RequestParam("checked") Integer check) {
        cartService.checkItem(skuId,check);
        return "redirect:http://cart.gulimail.com/cart.html";
    }
    @GetMapping("/deleteItem")
    public String deleteItem(@RequestParam("skuId") Long skuId) {
        cartService.deleteItem(skuId);
        return "redirect:http://cart.gulimail.com/cart.html";
    }

    /**
     * 根据用户id获取购物车信息
     * @param userId
     * @return
     */
    @GetMapping("/{userId}")
    @ResponseBody
    public List<CartItemVo> getCarsByUserId(@PathVariable Long userId) {
        List<CartItemVo> carts = cartService.getCarsByUserId(userId);
        return carts;
    }
}
