package com.atguigu.gulimail.ware.vo;

import lombok.Data;

import java.util.List;

@Data
public class MergePurchaseVo {
    public Long purchaseId;
    public List<Long> items;
}
