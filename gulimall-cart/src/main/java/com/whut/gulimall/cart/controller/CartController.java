package com.whut.gulimall.cart.controller;


import com.alibaba.fastjson.JSON;
import com.whut.gulimall.cart.service.CartService;
import com.whut.gulimall.cart.vo.Cart;
import com.whut.gulimall.cart.vo.CartItem;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.concurrent.ExecutionException;
@Slf4j
@Controller
public class CartController {

    @Autowired
    CartService cartService;

    @ResponseBody
    @GetMapping("/getCountNum")
    public Integer getCountNum() throws ExecutionException, InterruptedException {
        return cartService.getCart().getCountNum();
    }

    @GetMapping("/currentUserCartItems")
    @ResponseBody
    public List<CartItem> getCurrentUserCartItems(){
        return cartService.getCurrentUserCartItems();
    }

    @GetMapping("/deleteItem")
    public String deleteItem(@Param("skuId") Long skuId){
        cartService.deleteItem(skuId);
        return "redirect:http://cart.gulimall.com/cart_index";
    }

    @GetMapping("/countItem")
    public String countItem(@Param("skuId") Long skuId,
                            @Param("num")  Integer num){
        cartService.changeItemCount(skuId,num);
        return "redirect:http://cart.gulimall.com/cart_index";
    }

    @GetMapping("/checkItem")
    public String checkItem(@Param("skuId") Long skuId,
                            @Param("check")  Integer check){
        cartService.checkItem(skuId,check);
        return "redirect:http://cart.gulimall.com/cart_index";
    }

    @GetMapping("/cart_index")
    public String cartListPage(Model model) throws ExecutionException, InterruptedException {
        Cart cart = cartService.getCart();
        model.addAttribute("cart",cart);
        return "cartList";
    }

    /**
     * 添加商品到购物车
     * @return
     */
    @GetMapping("/addToCart")
    public String addToCart(@RequestParam("skuId") Long skuId,
                            @RequestParam("num") Integer num,
                            RedirectAttributes redirectAttributes) throws ExecutionException, InterruptedException {
        cartService.addToCart(skuId, num);
        redirectAttributes.addAttribute("skuId",skuId);
        return "redirect:http://cart.gulimall.com/addToCartSuccess";
    }

    /**
     * 跳转到添加成功页面
     * @param skuId
     * @param model
     * @return
     */
    @GetMapping("/addToCartSuccess")
    public String addToCartSuccessPage(@RequestParam("skuId") Long skuId, Model model){
        // 重定向到成功页面，再次查询购物车数据
        CartItem cartItem = cartService.getCartItem(skuId);
        model.addAttribute("cartItem",cartItem);
        return "success";
    }



}
