package com.atguigu.gulimail.product.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * ??Ʒ?
 * 
 * @author zhangtianyu
 * @email 937027255@qq.com
 * @date 2022-07-08 14:31:00
 */
@Data
@TableName("pms_attr")
public class AttrEntity implements Serializable {
	private static final long serialVersionUID = 1L;


	@TableId
	private Long attrId;

	private String attrName;

	private Integer searchType;

	private String icon;

	private String valueSelect;

	private Integer attrType;

	private Long enable;

	private Long catelogId;

	private Integer showDesc;

}
