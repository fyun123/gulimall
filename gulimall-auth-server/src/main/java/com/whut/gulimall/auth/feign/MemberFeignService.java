package com.whut.gulimall.auth.feign;

import com.whut.common.to.SocialUserTo;
import com.whut.common.to.UserLoginTo;
import com.whut.common.utils.R;
import com.whut.gulimall.auth.vo.UserRegisterVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("gulimall-member")
public interface MemberFeignService {

    @PostMapping("/member/member/register")
    R register(@RequestBody UserRegisterVo vo);

    @PostMapping("/member/member/login")
    R login(@RequestBody UserLoginTo to);

    @PostMapping("/member/member/oauth/login")
    R socialLogin(@RequestBody SocialUserTo socialUserTo) throws Exception;
}
