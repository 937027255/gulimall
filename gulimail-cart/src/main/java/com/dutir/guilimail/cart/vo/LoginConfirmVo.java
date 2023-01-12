package com.dutir.guilimail.cart.vo;

import lombok.Data;

@Data
public class LoginConfirmVo {
    private Long userId;
    private String userKey;
    private boolean isFirstTempt = false;
}
