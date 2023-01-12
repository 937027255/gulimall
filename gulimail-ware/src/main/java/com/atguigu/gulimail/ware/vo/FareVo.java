package com.atguigu.gulimail.ware.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @author zr
 * @date 2021/12/24 9:49
 */
@Data
public class FareVo {
    private MemberAddressVo address;
    private BigDecimal fare;
}
