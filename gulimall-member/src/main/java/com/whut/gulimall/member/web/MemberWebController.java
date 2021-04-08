package com.whut.gulimall.member.web;

import com.alibaba.fastjson.JSON;
import com.whut.common.utils.R;
import com.whut.gulimall.member.feign.OrderFeignService;
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

    @GetMapping("/memberOrderList.html")
    public String orderListPage(@RequestParam(value = "pageNum",defaultValue = "1") Integer pageNum, Model model){
        Map<String , Object> map = new HashMap<>();
        map.put("page",pageNum);
        R r = orderFeignService.listWithItem(map);
        // 只有登录的用户才能访问，请求头带上cookie就认为已经登录，需要配置feign请求拦截器，
        model.addAttribute("orders",r);
        return "orderList";
    }
}
