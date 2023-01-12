package com.atguigu.gulimail.coupon.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * ??ҳ?ֲ????
 * 
 * @author zhangtianyu
 * @email 937027255@qq.com
 * @date 2022-07-08 19:59:13
 */
@Data
@TableName("sms_home_adv")
public class HomeAdvEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@TableId
	private Long id;
	/**
	 * ???
	 */
	private String name;
	/**
	 * ͼƬ??ַ
	 */
	private String pic;
	/**
	 * ??ʼʱ?
	 */
	private Date startTime;
	/**
	 * ????ʱ?
	 */
	private Date endTime;
	/**
	 * ״̬
	 */
	private Integer status;
	/**
	 * ??????
	 */
	private Integer clickCount;
	/**
	 * ???????????ӵ?ַ
	 */
	private String url;
	/**
	 * ??ע
	 */
	private String note;
	/**
	 * ???
	 */
	private Integer sort;
	/**
	 * ?????
	 */
	private Long publisherId;
	/**
	 * ?????
	 */
	private Long authId;

}
