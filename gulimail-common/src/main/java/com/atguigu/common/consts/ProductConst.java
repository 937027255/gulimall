package com.atguigu.common.consts;

public enum ProductConst {

    ATTR_TYPE_BASE(1,"基本属性"),
    ATTR_TYPE_SALE(0,"销售属性");

    private int code;
    private String msg;

    ProductConst(int code,String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
