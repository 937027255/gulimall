package com.dutir.guilimail.cart.service;

import com.dutir.guilimail.cart.vo.CartItemVo;
import com.dutir.guilimail.cart.vo.CartVo;

import java.util.List;
import java.util.concurrent.ExecutionException;

public interface CartService {
    /**
     * 将某个商品添加到购物车内
     * @param skuId
     * @param num
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    CartItemVo addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException;

    /**
     * 获取购物项
     * @param skuId
     * @return
     */
    CartItemVo getCartItem(Long skuId);

    /**
     * 获取购物车的全部购物项内容
     * @param userKey
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    CartVo getCarts(String userKey) throws ExecutionException, InterruptedException;

    /**
     * 选中或者清除选中购物车当前商品状态
     * @param skuId
     * @param check 0是清除，1是选中
     */
    void checkItem(Long skuId, Integer check);

    /**
     * 删除购物车某个商品
     * @param skuId
     */
    void deleteItem(Long skuId);

    /**
     * 通过用户id查找所有的购物项
     * @param userId
     * @return
     */
    List<CartItemVo> getCarsByUserId(Long userId);
}
