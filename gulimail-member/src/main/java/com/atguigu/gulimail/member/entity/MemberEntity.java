package com.atguigu.gulimail.member.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * ??Ա
 * 
 * @author zhangtianyu
 * @email 937027255@qq.com
 * @date 2022-07-08 20:06:28
 */
@Data
@TableName("ums_member")
public class MemberEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	@TableId
	private Long id;

	private Long levelId;

	private String username;

	private String password;

	private String nickname;

	private String mobile;

	private String email;

	private String header;

	private Integer gender;

	private Date birth;

	private String city;

	private String job;

	private String sign;

	private Integer sourceType;

	private Integer integration;

	private Integer growth;

	private Integer status;

	private Date createTime;

}
