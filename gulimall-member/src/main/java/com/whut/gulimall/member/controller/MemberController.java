package com.whut.gulimall.member.controller;

import java.util.Arrays;
import java.util.Map;

//import org.apache.shiro.authz.annotation.RequiresPermissions;
import com.whut.common.exception.BizCodeEnume;
import com.whut.common.to.SocialUserTo;
import com.whut.common.to.UserLoginTo;
import com.whut.gulimall.member.exception.PhoneExistException;
import com.whut.gulimall.member.exception.UsernameExistException;
import com.whut.gulimall.member.vo.MemberRegisterVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.whut.gulimall.member.entity.MemberEntity;
import com.whut.gulimall.member.service.MemberService;
import com.whut.common.utils.PageUtils;
import com.whut.common.utils.R;



/**
 * 会员
 *
 * @author fangyun
 * @email fangyun@gmail.com
 * @date 2021-01-23 21:12:25
 */
@RestController
@RequestMapping("member/member")
public class MemberController {
    @Autowired
    private MemberService memberService;

    /**
     * 注册
     */
    @PostMapping("/register")
    public R register(@RequestBody MemberRegisterVo vo){
        try {
            memberService.register(vo);
        } catch (PhoneExistException e){
            return R.error(BizCodeEnume.PHONE_EXIST_EXCEPTION.getCode(),BizCodeEnume.PHONE_EXIST_EXCEPTION.getMsg());
        } catch (UsernameExistException e){
            return R.error(BizCodeEnume.USER_EXIST_EXCEPTION.getCode(),BizCodeEnume.USER_EXIST_EXCEPTION.getMsg());
        }

        return R.ok();
    }

    /**
     * 登陆
     */
    @PostMapping("/login")
    public R login(@RequestBody UserLoginTo to){
        MemberEntity memberEntity = memberService.login(to);
        if (memberEntity != null){
            return R.ok().setData(memberEntity);
        }else {
            return R.error(BizCodeEnume.LOGINACCOUNT_PASSWORD_INVALID_EXCEPTION.getCode(),BizCodeEnume.LOGINACCOUNT_PASSWORD_INVALID_EXCEPTION.getMsg());
        }
    }

    /**
     * 社交用户登陆
     */
    @PostMapping("/oauth/login")
    public R socialLogin(@RequestBody SocialUserTo socialUserTo) throws Exception {
        MemberEntity memberEntity = memberService.login(socialUserTo);
        if (memberEntity != null){
            return R.ok().setData(memberEntity);
        }else {
            return R.error(BizCodeEnume.LOGINACCOUNT_PASSWORD_INVALID_EXCEPTION.getCode(),BizCodeEnume.LOGINACCOUNT_PASSWORD_INVALID_EXCEPTION.getMsg());
        }
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = memberService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id){
		MemberEntity member = memberService.getById(id);

        return R.ok().put("member", member);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody MemberEntity member){
		memberService.save(member);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody MemberEntity member){
		memberService.updateById(member);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] ids){
		memberService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
