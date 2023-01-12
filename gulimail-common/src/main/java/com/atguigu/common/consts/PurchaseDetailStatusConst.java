package com.atguigu.common.consts;

public enum PurchaseDetailStatusConst {
    CREATED(0,"新建"),
    ASSIGNED(1,"已分配"),
    BUYIONG(2,"正在采购"),
    FINISHED(3,"已完成"),
    FAILED(4,"采购失败");
    private int code;
    private String msg;
    PurchaseDetailStatusConst(int code,String msg) {
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
