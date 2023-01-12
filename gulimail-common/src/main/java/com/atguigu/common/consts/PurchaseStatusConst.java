package com.atguigu.common.consts;

public enum PurchaseStatusConst {

    CREATED(0,"新建"),
    ASSIGNED(1,"已分配"),
    RECEIVED(2,"已领取"),
    FINISHED(3,"已完成"),
    ERROR(4,"出现异常");
    private int code;
    private String msg;
    PurchaseStatusConst(int code,String msg) {
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
