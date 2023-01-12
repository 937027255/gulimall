package com.atguigu.gulimail.product.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * spu????ֵ
 * 
 * @author zhangtianyu
 * @email 937027255@qq.com
 * @date 2022-07-08 14:31:00
 */
@Data
@TableName("pms_product_attr_value")
public class ProductAttrValueEntity implements Serializable {
	private static final long serialVersionUID = 1L;


	@TableId
	private Long id;

	private Long spuId;

	private Long attrId;

	private String attrName;

	private String attrValue;

	private Integer attrSort;

	private Integer quickShow;

}
