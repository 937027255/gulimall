package com.atguigu.gulimail.order.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

import lombok.Data;

/**
 * ????
 * 
 * @author zhangtianyu
 * @email 937027255@qq.com
 * @date 2022-07-08 20:13:28
 */
@Data
@TableName("oms_order")
public class OrderEntity implements Serializable {
	private static final long serialVersionUID = 1L;


	@TableId
	private Long id;

	private Long memberId;

	private String orderSn;

	private Long couponId;

	private Date createTime;

	private String memberUsername;

	private BigDecimal totalAmount;

	private BigDecimal payAmount;

	private BigDecimal freightAmount;

	private BigDecimal promotionAmount;

	private BigDecimal integrationAmount;

	private BigDecimal couponAmount;

	private BigDecimal discountAmount;

	private Integer payType;

	private Integer sourceType;

	private Integer status;

	private String deliveryCompany;

	private String deliverySn;

	private Integer autoConfirmDay;

	private Integer integration;

	private Integer growth;

    /**
     * 发票信息
     */
	private Integer billType;

	private String billHeader;

	private String billContent;

	private String billReceiverPhone;

	private String billReceiverEmail;

    /**
     * 收货人信息
     */
	private String receiverName;

	private String receiverPhone;

	private String receiverPostCode;

	private String receiverProvince;

	private String receiverCity;

	private String receiverRegion;

	private String receiverDetailAddress;

	private String note;

	private Integer confirmStatus;

	private Integer deleteStatus;

	private Integer useIntegration;

    /**
     * 日期
     */
	private Date paymentTime;

	private Date deliveryTime;

	private Date receiveTime;

	private Date commentTime;

	private Date modifyTime;

    @TableField(exist = false)
    private List<OrderItemEntity> itemEntities;

}
