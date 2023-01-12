package com.dutir.guilimail.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.R;
import com.dutir.guilimail.cart.feign.ProductFeignService;
import com.dutir.guilimail.cart.interceptor.GulimailInterceptor;
import com.dutir.guilimail.cart.service.CartService;
import com.dutir.guilimail.cart.vo.CartItemVo;
import com.dutir.guilimail.cart.vo.CartVo;
import com.dutir.guilimail.cart.vo.LoginConfirmVo;
import com.dutir.guilimail.cart.vo.SkuInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CartServiceImpl implements CartService {

    private final String CART_PREFIX = "gulimail:cart";

    @Autowired
    StringRedisTemplate redisTemplate;
    @Autowired
    ProductFeignService productFeignService;
    @Autowired
    ThreadPoolExecutor executor;

    /**
     * 添加到购物车操作
     * @param skuId
     * @param num
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Override
    public CartItemVo addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();

        if (cartOps.get(skuId.toString()) == null) {
            //对于第一次添加到购物车的内容
            CartItemVo cartItemVo = new CartItemVo();
            //通过skuId去product模块查询出来具体的信息
            CompletableFuture<Void> skuFuture = CompletableFuture.runAsync(() -> {
                R r = productFeignService.getSkuInfo(skuId);
                if (r.getCode() == 0) {
                    //远程调用成功将数据解封出来
                    SkuInfoVo skuInfo = (SkuInfoVo) r.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                    });
                    //将skuinfo内容封装到cartItemVo中去
                    cartItemVo.setCheck(true);
                    cartItemVo.setSkuId(skuId);
                    cartItemVo.setCount(num);
                    cartItemVo.setTitle(skuInfo.getSkuTitle());
                    cartItemVo.setImage(skuInfo.getSkuDefaultImg());
                    cartItemVo.setPrice(skuInfo.getPrice());
                }
            }, executor);
            CompletableFuture<Void> saleAttrFuture = CompletableFuture.runAsync(() -> {
                //远程调用查询该skuId对应的所有销售属性
                List<String> saleAttrList = productFeignService.getSaleAttrList(skuId);
                cartItemVo.setSkuAttrValues(saleAttrList);
            }, executor);

            //数据全部获取获取到之后
            CompletableFuture.allOf(skuFuture,saleAttrFuture).get();
            //放入redis进行返回
            String s = JSON.toJSONString(cartItemVo);
            cartOps.put(skuId.toString(),s);

            return cartItemVo;
        }
        else {
            String cartString = (String) cartOps.get(skuId.toString());
            CartItemVo cartItemVo = JSON.parseObject(cartString, CartItemVo.class);
            cartItemVo.setCount(cartItemVo.getCount() + num);
            cartOps.put(skuId.toString(),JSON.toJSONString(cartItemVo));
            return cartItemVo;
        }
    }

    @Override
    public CartItemVo getCartItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        String s = (String) cartOps.get(skuId.toString());
        CartItemVo cartItemVo = JSON.parseObject(s, CartItemVo.class);
        return cartItemVo;
    }

    @Override
    public CartVo getCarts(String userKey) throws ExecutionException, InterruptedException {
        CartVo cartVo = new CartVo();
        //判断是登陆的用户还是没有登陆的用户
        LoginConfirmVo loginConfirmVo = GulimailInterceptor.threadLocal.get();
        if (loginConfirmVo.getUserId() != null) {
            //用户已经登陆 -> 需要将临时用户的购物车信息合并到登陆的用户中，并清除临时用户在redis中的数据
            String redisKey = CART_PREFIX + loginConfirmVo.getUserId();
            if (userKey!= null) {
                //首先获取到临时用户的购物车数据
                String tempUserKey = CART_PREFIX + userKey;
                List<CartItemVo> tempUserCarts = getCartsByKey(tempUserKey);
                if (tempUserCarts != null) {
                    for (CartItemVo tempUserCart : tempUserCarts) {
                        addToCart(tempUserCart.getSkuId(),tempUserCart.getCount());
                    }
                    redisTemplate.delete(tempUserKey);
                }
            }
            List<CartItemVo> carts = getCartsByKey(redisKey);
            cartVo.setItems(carts);
            return cartVo;
        } else {
            //用户没有登陆
            //查询临时用户的信息封装返回
            String redisKey = CART_PREFIX + loginConfirmVo.getUserKey();
            List<CartItemVo> carts = getCartsByKey(redisKey);
            cartVo.setItems(carts);
        }
        return cartVo;
    }

    @Override
    public void checkItem(Long skuId, Integer check) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        CartItemVo cartItem = getCartItem(skuId);
        cartItem.setCheck(check==1?true:false);
        String s = JSON.toJSONString(cartItem);
        cartOps.put(skuId.toString(),s);
    }

    @Override
    public void deleteItem(Long skuId) {
        getCartOps().delete(skuId.toString());
    }

    @Override
    public List<CartItemVo> getCarsByUserId(Long userId) {
        List<CartItemVo> carts = getCartsByKey(CART_PREFIX + userId.toString());
        //对当前用户下所有的购物项做过滤操作，如果没有选中就过滤掉
        List<CartItemVo> collect = carts.stream().filter(item -> {
            return item.getCheck();
        }).map(item -> {
            //远程重新查询一次价格，不能用redis中保存的价格
            Long skuId = item.getSkuId();
            R r = productFeignService.getSkuInfo(skuId);
            if (r.getCode() == 0) {
                SkuInfoVo skuInfo = (SkuInfoVo) r.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                });
                item.setPrice(skuInfo.getPrice());
            }
            return item;
        }).collect(Collectors.toList());
        return collect;
    }

    private BoundHashOperations<String, Object, Object> getCartOps() {
        LoginConfirmVo loginConfirmVo = GulimailInterceptor.threadLocal.get();
        String redisKey = "";
        if (loginConfirmVo.getUserId() != null) {
            //说明用户已经登陆
            redisKey = CART_PREFIX + loginConfirmVo.getUserId();
        } else {
            //使用临时用户登陆
            redisKey = CART_PREFIX + loginConfirmVo.getUserKey();
        }
        BoundHashOperations<String, Object, Object> cartOps = redisTemplate.boundHashOps(redisKey);
        return cartOps;
    }

    /**
     * 通过redis的key查找所有购物项的集合
     * @param key
     * @return
     */
    public  List<CartItemVo> getCartsByKey(String key) {
        //通过key获取到所有的value后封装返回
        BoundHashOperations<String, Object, Object> cartOps = redisTemplate.boundHashOps(key);
        List<Object> values = cartOps.values();
        if (values != null && values.size() > 0) {
            List<CartItemVo> carts = values.stream().map(cart -> {
                String str = (String) cart;
                CartItemVo cartItemVo = JSON.parseObject(str, CartItemVo.class);
                return cartItemVo;
            }).collect(Collectors.toList());
            return carts;
        }
        return null;
    }
}
