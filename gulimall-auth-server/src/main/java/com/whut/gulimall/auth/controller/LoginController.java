package com.whut.gulimall.auth.controller;

import com.alibaba.fastjson.TypeReference;
import com.whut.common.constant.AuthServerConstant;
import com.whut.common.exception.BizCodeEnume;
import com.whut.common.to.UserLoginTo;
import com.whut.common.utils.R;
import com.whut.common.vo.MemberResVo;
import com.whut.gulimall.auth.feign.MemberFeignService;
import com.whut.gulimall.auth.feign.ThirdPartyFeignService;
import com.whut.gulimall.auth.vo.UserRegisterVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
@Slf4j
@Controller
public class LoginController {

    @Autowired
    private MemberFeignService memberFeignService;

    @Autowired
    ThirdPartyFeignService thirdPartyFeignService;

    @Autowired
    StringRedisTemplate redisTemplate;


    /**
     * 发送验证码
     * 1. 接口防刷
     * 2. 验证码再次校验
     */
    @ResponseBody
    @GetMapping("/sms/sendCode")
    public R sendCode(@RequestParam("phone") String phone) {

        String redisCode = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone);
        if (!StringUtils.isEmpty(redisCode)) {
            long l = Long.parseLong(redisCode.split("_")[1]);
            if (System.currentTimeMillis() - l < 60000) {
                // 60s内不能再发
                return R.error(BizCodeEnume.VALID_SMS_CODE_EXCEPTION.getCode(), BizCodeEnume.VALID_SMS_CODE_EXCEPTION.getMsg());
            }
        }
        // 2. 验证码再次校验sms:code:phoneNum =>code
        String code = UUID.randomUUID().toString().substring(0, 5);
        String s = code + "_" + System.currentTimeMillis();

        // redis缓存验证码，防止同一个手机号，60s内再次发送验证码
        redisTemplate.opsForValue().set(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone, s, 5, TimeUnit.MINUTES);
        thirdPartyFeignService.sendCode(phone, code);
        return R.ok();
    }

    /**
     * 注册
     */
    @PostMapping("/register")
    public String register(@Valid UserRegisterVo vo, BindingResult result, RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            Map<String, String> errors = result.getFieldErrors().stream().collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.gulimall.com/reg.html";
        }
        // 注册
        // 1.校验验证码
        String code = vo.getCode();
        String s = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());
        if (!StringUtils.isEmpty(s) && code.equals(s.split("_")[0])) {
            // 删除验证码
            redisTemplate.delete("AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone()");
            // 验证码校验成功
            R r = memberFeignService.register(vo);
            if (r.getCode() == 0) {
                // 注册成功
                return "redirect:http://auth.gulimall.com/login.html";
            } else {
                Map<String, String> errors = new HashMap<>();
                errors.put("code", r.getData("msg",new TypeReference<String>(){}));
                redirectAttributes.addFlashAttribute("errors",errors);
                return "redirect:http://auth.gulimall.com/reg.html";
            }

        } else {
            Map<String, String> errors = new HashMap<>();
            errors.put("code", "验证码错误");
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.gulimall.com/reg.html";
        }
    }

    @GetMapping("/login.html")
    public String loginPage(HttpSession session){
        Object attribute = session.getAttribute(AuthServerConstant.LOGIN_USER);
        if (attribute == null) {
            return "login";
        } else {
            return "redirect:http://gulimall.com";
        }
    }


    /**
     * @param to 页面表单Key-Value
     * @return
     */
    @PostMapping("/login")
    public String login(UserLoginTo to, RedirectAttributes redirectAttributes, HttpSession session) {
        //远程登陆
        R login = memberFeignService.login(to);
        if (login.getCode() == 0) {
            MemberResVo data = login.getData("data", new TypeReference<MemberResVo>() {
            });
            log.info("登录成功！用户信息"+data.toString());
            session.setAttribute(AuthServerConstant.LOGIN_USER,data);
            return "redirect:http://gulimall.com";
        } else {
            Map<String, String> errors = new HashMap<>();
            errors.put("msg", login.getData("msg", new TypeReference<String>() {
            }));
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.gulimall.com/login.html";
        }

    }
}
