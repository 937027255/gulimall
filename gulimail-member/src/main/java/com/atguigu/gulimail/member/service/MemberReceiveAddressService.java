package com.atguigu.gulimail.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimail.member.entity.MemberReceiveAddressEntity;

import java.util.List;
import java.util.Map;

/**
 * ??Ա?ջ???ַ
 *
 * @author zhangtianyu
 * @email 937027255@qq.com
 * @date 2022-07-08 20:06:28
 */
public interface MemberReceiveAddressService extends IService<MemberReceiveAddressEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * 通过memberId查询地址列表
     * @param memberId
     * @return
     */
    List<MemberReceiveAddressEntity> getAddressesByMemberId(Long memberId);
}

