package com.dutir.guilimail.cart.service;

import com.dutir.guilimail.cart.vo.CartItemVo;
import com.dutir.guilimail.cart.vo.CartVo;

import java.util.List;
import java.util.concurrent.ExecutionException;

public interface CartService {
    CartItemVo addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException;

    CartItemVo getCartItem(Long skuId);

    CartVo getCarts(String userKey) throws ExecutionException, InterruptedException;

    void checkItem(Long skuId, Integer check);

    void deleteItem(Long skuId);

    /**
     * 通过用户id查找所有的购物项
     * @param userId
     * @return
     */
    List<CartItemVo> getCarsByUserId(Long userId);
}
