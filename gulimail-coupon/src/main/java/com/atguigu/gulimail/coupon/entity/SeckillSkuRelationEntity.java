package com.atguigu.gulimail.coupon.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * ??ɱ???Ʒ????
 * 
 * @author zhangtianyu
 * @email 937027255@qq.com
 * @date 2022-07-08 19:59:13
 */
@Data
@TableName("sms_seckill_sku_relation")
public class SeckillSkuRelationEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@TableId
	private Long id;

	private Long promotionId;

	private Long promotionSessionId;

	private Long skuId;

	private BigDecimal seckillPrice;

	private BigDecimal seckillCount;

	private BigDecimal seckillLimit;

	private Integer seckillSort;

}
