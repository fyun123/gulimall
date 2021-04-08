package com.whut.gulimall.auth.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.whut.common.constant.AuthServerConstant;
import com.whut.common.to.SocialUserTo;
import com.whut.common.utils.HttpUtils;
import com.whut.common.utils.R;
import com.whut.common.vo.MemberResVo;
import com.whut.gulimall.auth.feign.MemberFeignService;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * 处理社交登陆请求
 */
@Slf4j
@Controller
public class OAuth2Controller {

    @Autowired
    MemberFeignService memberFeignService;

    @PostMapping("/oauth2.0/qq/success")
    public String qq(@RequestParam("code") String code) throws Exception {
        // 通过Authorization Code获取Access Token
        Map<String, String> map = new HashMap<>();
        map.put("grant_type","authorization_code");
        map.put("client_id",""); //app id
        map.put("client_secret",""); //app key
        map.put("code",code);
        map.put("redirect_uri","http://auth.gulimall.com/oauth2.0/qq/success");
        HttpResponse response = HttpUtils.doGet("graph.qq.com", "/oauth2.0/token", "GET", null, map);
        // 处理
        if (response.getStatusLine().getStatusCode() == 200){
            // 获取Access Token
            String json = EntityUtils.toString(response.getEntity());
            SocialUserTo socialUserTo;
            socialUserTo = JSON.parseObject(json, SocialUserTo.class);
            // 获取用户uuid
            Map<String, String> idMap = new HashMap<>();
            idMap.put("access_token",socialUserTo.getAccessToken());
            HttpResponse id = HttpUtils.doGet("graph.qq.com", "/oauth2.0/me", "GET", null, idMap);
            String idJson = EntityUtils.toString(id.getEntity());
            JSONObject jsonObject = JSON.parseObject(idJson);
//            jsonObject.getString("")
//            socialUserTo.setUid();

            // 1. 第一次登入自动注册（为当前社交用户生成一个账号）
        } else {
            return "redirect:http://auth.gulimall.com/login.html";
        }


        // 登陆成功，跳回首页
        return "";
    }

    @GetMapping("/oauth2.0/weibo/success")
    public String weibo(@RequestParam("code") String code, HttpSession session, HttpServletResponse servletResponse) throws Exception {

        // 通过Authorization Code获取Access Token
        Map<String, String> map = new HashMap<>();
        map.put("grant_type","authorization_code");
        map.put("client_id","2151549418"); //app id
        map.put("client_secret","feb686948ea7a3c3afb1a36c5c256c85"); //app key
        map.put("code",code);
        map.put("redirect_uri","http://auth.gulimall.com/oauth2.0/weibo/success");
        HttpResponse res = HttpUtils.doPost("https://api.weibo.com", "/oauth2/access_token", "post",new HashMap<String, String>(), map, (byte[]) null);
        // 处理
        if (res.getStatusLine().getStatusCode() == 200){
            // 获取Access Token
            String json = EntityUtils.toString(res.getEntity());
            SocialUserTo socialUserTo = JSON.parseObject(json, SocialUserTo.class);
            // 1. 第一次登入自动注册（为当前社交用户生成一个账号）
            R r = memberFeignService.socialLogin(socialUserTo);
            if (r.getCode() == 0) {
                MemberResVo data = r.getData("data", new TypeReference<MemberResVo>() {
                });
                log.info("登录成功!用户信息:{}",data.toString());
                session.setAttribute(AuthServerConstant.LOGIN_USER,data);
                return "redirect:http://gulimall.com";
            } else {
                return "redirect:http://auth.gulimall.com/login.html";
            }
        } else {
            return "redirect:http://auth.gulimall.com/login.html";
        }
    }
}
