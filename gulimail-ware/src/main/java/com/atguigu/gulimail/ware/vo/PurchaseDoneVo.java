package com.atguigu.gulimail.ware.vo;

import lombok.Data;

import java.util.List;


@Data
public class PurchaseDoneVo {
    private Long id;//采购单的id
    private List<PurchaseDetailVo> items;//采购单对应的采购明细
}
