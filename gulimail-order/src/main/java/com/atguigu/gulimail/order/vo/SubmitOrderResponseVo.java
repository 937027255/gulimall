package com.atguigu.gulimail.order.vo;

import com.atguigu.gulimail.order.entity.OrderEntity;
import lombok.Data;


/**
 * 提交订单返回结果
 * @author zr
 * @date 2021/12/23 22:17
 */
@Data
public class SubmitOrderResponseVo {
    private OrderEntity order;

    /** 错误状态码 0成功**/
    private Integer code;
}
