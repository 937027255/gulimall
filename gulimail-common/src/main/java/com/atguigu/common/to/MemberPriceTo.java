package com.atguigu.common.to;

import lombok.Data;

import java.util.List;

@Data
public class MemberPriceTo {
    private Long skuId;
    private List<MemberPrice> memberPrice;
}
