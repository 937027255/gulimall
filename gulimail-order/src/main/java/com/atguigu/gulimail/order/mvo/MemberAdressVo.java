package com.atguigu.gulimail.order.mvo;

import lombok.Data;

@Data
public class MemberAdressVo {
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
