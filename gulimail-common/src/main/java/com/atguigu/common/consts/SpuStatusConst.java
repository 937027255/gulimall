package com.atguigu.common.consts;

public enum SpuStatusConst {
    CREATED(0,"新建"),
    SPU_UP(1,"商品上架"),
    SPU_DOWN(2,"商品下架");
    private int code;
    private String msg;
    SpuStatusConst(int code, String msg) {
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
