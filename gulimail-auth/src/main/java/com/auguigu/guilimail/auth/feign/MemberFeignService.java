package com.auguigu.guilimail.auth.feign;

import com.atguigu.common.utils.R;
import com.auguigu.guilimail.auth.vo.MemberRegisterVo;
import com.auguigu.guilimail.auth.vo.UserLoginVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient("gulimail-member")
public interface MemberFeignService {
    @PostMapping("/member/member/register")
    public R regist(@RequestBody MemberRegisterVo memberRegisterVo);
    @PostMapping("/member/member/login") // member
    public R login(@RequestBody UserLoginVo loginVo);
}
