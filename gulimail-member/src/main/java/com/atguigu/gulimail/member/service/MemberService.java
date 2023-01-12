package com.atguigu.gulimail.member.service;

import com.atguigu.gulimail.member.vo.MemberLoginVo;
import com.atguigu.gulimail.member.vo.MemberRegisterVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimail.member.entity.MemberEntity;

import java.util.Map;

/**
 * ??Ա
 *
 * @author zhangtianyu
 * @email 937027255@qq.com
 * @date 2022-07-08 20:06:28
 */
public interface MemberService extends IService<MemberEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void regist(MemberRegisterVo userRegisterVo);

    MemberEntity login(MemberLoginVo loginVo);
}

