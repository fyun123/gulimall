package com.whut.gulimall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.whut.common.to.SocialUserTo;
import com.whut.common.to.UserLoginTo;
import com.whut.common.utils.PageUtils;
import com.whut.gulimall.member.entity.MemberEntity;
import com.whut.gulimall.member.entity.MemberLevelEntity;
import com.whut.gulimall.member.exception.PhoneExistException;
import com.whut.gulimall.member.exception.UsernameExistException;
import com.whut.gulimall.member.vo.MemberRegisterVo;

import java.util.Map;

/**
 * 会员
 *
 * @author fangyun
 * @email fangyun@gmail.com
 * @date 2021-01-23 21:12:25
 */
public interface MemberService extends IService<MemberEntity> {

    PageUtils queryPage(Map<String, Object> params);

    MemberEntity login(UserLoginTo to);

    void register(MemberRegisterVo vo);

    void checkPhoneUnique(String phone) throws PhoneExistException;

    void checkUsernameUnique(String username) throws UsernameExistException;

    MemberEntity login(SocialUserTo socialUserTo) throws Exception;
}

