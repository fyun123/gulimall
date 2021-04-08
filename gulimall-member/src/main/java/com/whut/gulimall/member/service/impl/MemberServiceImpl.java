package com.whut.gulimall.member.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.whut.common.to.SocialUserTo;
import com.whut.common.to.UserLoginTo;
import com.whut.common.utils.HttpUtils;
import com.whut.gulimall.member.dao.MemberLevelDao;
import com.whut.gulimall.member.entity.MemberLevelEntity;
import com.whut.gulimall.member.exception.PhoneExistException;
import com.whut.gulimall.member.exception.UsernameExistException;
import com.whut.gulimall.member.vo.MemberRegisterVo;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.whut.common.utils.PageUtils;
import com.whut.common.utils.Query;

import com.whut.gulimall.member.dao.MemberDao;
import com.whut.gulimall.member.entity.MemberEntity;
import com.whut.gulimall.member.service.MemberService;

import javax.annotation.Resource;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {

    @Resource
    MemberLevelDao memberLevelDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public MemberEntity login(UserLoginTo to) {

        String loginAccount = to.getLoginAccount();
        String password = to.getPassword();
        // 1. 去数据库查询
        MemberEntity memberEntity = this.baseMapper.selectOne(new QueryWrapper<MemberEntity>().eq("username", loginAccount)
                .or().eq("mobile", loginAccount));
        if (memberEntity == null){
            return null;
        } else {
            String passwordDb = memberEntity.getPassword();
            BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
            boolean matches = bCryptPasswordEncoder.matches(password, passwordDb);
            if (matches){
                return memberEntity;
            } else {
                return  null;
            }
        }
    }

    @Override
    public void register(MemberRegisterVo vo) {
        MemberEntity memberEntity = new MemberEntity();
        //设置默认等级
        MemberLevelEntity memberLevelEntity = memberLevelDao.getDefaultLevel();
        memberEntity.setLevelId(memberLevelEntity.getId());
        //检查手机号或者用户名是否唯一，异常机制
        checkPhoneUnique(vo.getPhone());
        checkUsernameUnique(vo.getUserName());
        // 设置手机号
        memberEntity.setMobile(vo.getPhone());
        // 设置用户名
        memberEntity.setUsername(vo.getUserName());
        memberEntity.setNickname(vo.getUserName());

        // 设置密码。加密存储
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encode = passwordEncoder.encode(vo.getPassword());
        memberEntity.setPassword(encode);
        // 其他要保存的默认信息
        memberEntity.setCreateTime(new Date());
        baseMapper.insert(memberEntity);
    }

    @Override
    public void checkPhoneUnique(String phone) throws PhoneExistException {
        Integer mobile = this.baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("mobile", phone));
        if (mobile > 0) {
            throw new PhoneExistException();
        }
    }

    @Override
    public void checkUsernameUnique(String username) throws UsernameExistException {
        Integer user = this.baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("username", username));
        if (user > 0) {
            throw new UsernameExistException();
        }
    }

    @Override
    public MemberEntity login(SocialUserTo socialUserTo) throws Exception {
        // 登陆和注册合并逻辑
        String uid = socialUserTo.getUid();
        MemberDao memberDao = this.baseMapper;
        MemberEntity member = memberDao.selectOne(new QueryWrapper<MemberEntity>().eq("social_uid", uid));
        if (member != null){
            MemberEntity updateMember = new MemberEntity();
            updateMember.setId(member.getId());
            updateMember.setAccessToken(socialUserTo.getAccessToken());
            memberDao.updateById(updateMember);
            member.setAccessToken(socialUserTo.getAccessToken());
            return member;
        } else {
            // 没有查到当前社交用户，，需要注册
            MemberEntity register = new MemberEntity();
            try {
                // 查询当前社交用户的信息
                Map<String, String> query= new HashMap<>();
                query.put("access_token",socialUserTo.getAccessToken());
                query.put("uid",socialUserTo.getUid());
                HttpResponse response = HttpUtils.doGet("https://api.weibo.com", "/2/users/show.json", "GET", new HashMap<String, String>(), query);
                if (response.getStatusLine().getStatusCode() == 200) {
                    // 查询成功
                    String json = EntityUtils.toString(response.getEntity());
                    JSONObject jsonObject = JSON.parseObject(json);
                    // 获取用户信息
                    register.setUsername(jsonObject.getString("name"));
                    register.setNickname(jsonObject.getString("name"));
                    register.setGender("m".equals(jsonObject.getString("gender")) ?1:0);

                }
            }catch (Exception e){ }
            register.setSocialUid(socialUserTo.getUid());
            register.setAccessToken(socialUserTo.getAccessToken());
            register.setCreateTime(new Date());
            memberDao.insert(register);
            return register;
        }
    }

}