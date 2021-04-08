package com.whut.gulimall.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.whut.common.utils.R;
import com.whut.gulimall.cart.feign.ProductFeignService;
import com.whut.gulimall.cart.interceptor.CartInterceptor;
import com.whut.gulimall.cart.service.CartService;
import com.whut.gulimall.cart.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CartServiceImpl implements CartService {

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    ThreadPoolExecutor executor;

    private final String CART_PREFIX = "gulimall:cart:";

    @Override
    public CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();

        String res = (String) cartOps.get(skuId.toString());
        if (StringUtils.isEmpty(res)){
            CartItem cartItem = new CartItem();
            // 添加新商品到购物车
            cartItem.setCheck(true);
            cartItem.setSkuId(skuId);
            cartItem.setCount(num);
            // 1. 远程查询当前商品信息
            CompletableFuture<Void> getSkuInfoTask = CompletableFuture.runAsync(() -> {
                R info = productFeignService.info(skuId);
                SkuInfoVo skuInfo = info.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                });
                Long spuId = skuInfo.getSpuId();
                R r = productFeignService.spuInfo(spuId);
                SpuInfoVo spuInfo = r.getData("spuInfo", new TypeReference<SpuInfoVo>() {
                });
                cartItem.setWeight(spuInfo.getWeight());
                cartItem.setTitle(skuInfo.getSkuTitle());
                cartItem.setImage(skuInfo.getSkuDefaultImg());
                cartItem.setPrice(skuInfo.getPrice());
            },executor);
            // 2. 远程查询sku的销售组合信息
            CompletableFuture<Void> getSkuSaleAttrValues = CompletableFuture.runAsync(() -> {
                List<String> attrValues = productFeignService.getSkuSaleAttrValues(skuId);
                cartItem.setSkuAttr(attrValues);
            }, executor);
            CompletableFuture.allOf(getSkuInfoTask,getSkuSaleAttrValues).get();
            String string = JSON.toJSONString(cartItem);
            cartOps.put(skuId.toString(), string);
            cartOps.put(skuId.toString(), JSON.toJSONString(cartItem));
            return cartItem;
        } else {
            // 购物车有此商品
            CartItem cartItemOld = JSON.parseObject(res, CartItem.class);
            cartItemOld.setCount(cartItemOld.getCount() + num);
            cartOps.put(skuId.toString(), JSON.toJSONString(cartItemOld));
            return cartItemOld;
        }




    }

    @Override
    public CartItem getCartItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        String s = (String) cartOps.get(skuId.toString());
        return JSON.parseObject(s, CartItem.class);
    }

    @Override
    public Cart getCart() throws ExecutionException, InterruptedException {
        Cart cart = new Cart();
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        if (userInfoTo.getUserId() != null){
            // 1. 登录用户
            String cartKey = CART_PREFIX + userInfoTo.getUserId();
            // 1.1 临时购物车还没合并，进行合并
            String tempCartKey = CART_PREFIX + userInfoTo.getUserKey();
            List<CartItem> tempCartItems = getCartItems(tempCartKey);
            if (tempCartItems != null && tempCartItems.size() > 0) {
                for (CartItem item : tempCartItems) {
                    addToCart(item.getSkuId(),item.getCount());
                }
                clearCart(tempCartKey);
            }
            // 1.2 获取购物车数据，包括临时购物车数据
            List<CartItem> cartItems = getCartItems(cartKey);
            cart.setItems(cartItems);
        } else {
            // 2. 临时用户
            String cartKey = CART_PREFIX + userInfoTo.getUserKey();
            List<CartItem> cartItems = getCartItems(cartKey);
            cart.setItems(cartItems);
        }
        return cart;
    }

    @Override
    public void clearCart(String cartKey) {
        redisTemplate.delete(cartKey);
    }

    @Override
    public void checkItem(Long skuId, Integer check) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        CartItem cartItem = getCartItem(skuId);
        cartItem.setCheck(check == 1);
        cartOps.put(skuId.toString(), JSON.toJSONString(cartItem));
    }

    @Override
    public void changeItemCount(Long skuId, Integer num) {

        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        if (num == 0){
            cartOps.delete(skuId.toString());
        } else {
            CartItem cartItem = getCartItem(skuId);
            cartItem.setCount(num);
            cartOps.put(skuId.toString(), JSON.toJSONString(cartItem));
        }
    }

    @Override
    public void deleteItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.delete(skuId.toString());
    }

    @Override
    public List<CartItem> getCurrentUserCartItems() {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        if (userInfoTo.getUserId() == null) {
            return null;
        } else {
            String cartKey = CART_PREFIX + userInfoTo.getUserId();
            List<CartItem> cartItems = getCartItems(cartKey);
            List<CartItem> collect = cartItems.stream()
                    .filter((item) -> item.getCheck()).map(item->{
                        // 更新为最新价格
                        R r = productFeignService.getPrice(item.getSkuId());
                        BigDecimal price = r.getData("data", new TypeReference<BigDecimal>() {});
                        item.setPrice(price);
                        return item;
                    }).collect(Collectors.toList());
            return collect;
        }
    }

    /**
     * 获取到需要操作的购物车
     * @return
     */
    private BoundHashOperations<String, Object, Object> getCartOps() {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        String cartKey = "";
        if (userInfoTo.getUserId() != null) {
            cartKey = CART_PREFIX + userInfoTo.getUserId();
        } else {
            cartKey = CART_PREFIX + userInfoTo.getUserKey();
        }
        BoundHashOperations<String, Object, Object> operations = redisTemplate.boundHashOps(cartKey);
        return operations;
    }

    private List<CartItem> getCartItems(String cartKey) {
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(cartKey);
        List<Object> values = hashOps.values();
        if (values != null && values.size() > 0) {
            List<CartItem> collect = values.stream().map((obj) -> {
                String str = (String) obj;
                return JSON.parseObject(str, CartItem.class);
            }).collect(Collectors.toList());
            return collect;
        }
        return null;
    }
}
