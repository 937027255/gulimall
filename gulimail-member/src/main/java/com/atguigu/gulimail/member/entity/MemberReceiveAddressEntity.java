package com.atguigu.gulimail.member.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * ??Ա?ջ???ַ
 * 
 * @author zhangtianyu
 * @email 937027255@qq.com
 * @date 2022-07-08 20:06:28
 */
@Data
@TableName("ums_member_receive_address")
public class MemberReceiveAddressEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	@TableId
	private Long id;

	private Long memberId;

	private String name;

	private String phone;

	private String postCode;

	private String province;

	private String city;

	private String region;

	private String detailAddress;

	private String areacode;

	private Integer defaultStatus;

}
