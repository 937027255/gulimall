package com.atguigu.common.exception;

/**
 * 10：通用
 *      001：参数格式校验
 *      002：短信验证码频率太高
 * 11：商品
 * 12：订单
 * 13：购物车
 * 14：物流
 * 15：用户
 * 21:库存
 */
public enum BizCodeEnum {

    UNKNOW_EXCEPTION(10000,"系统未知异常"),
    VALID_EXCEPTION(10001,"参数格式校验失败"),
    SMS_CODE_EXCEPTION(10002,"验证码获取频率太高，稍后再试"),
    PRODDUCT_UP_EXCEPTION(11000,"商品上架异常"),
    USER_EXIST_EXCEPTION(15001,"用户已存在"),
    PHONE_EXIST_EXCEPTION(15002,"手机号已注册"), LOGINACCT_PASSWORD_EXCEPTION(15003, "用户名或密码错误"),
    NO_WARE_STOCK_EXCEPTION(21001,"剩余库存不足");


    private int code;
    private String msg;

    BizCodeEnum(int code,String msg) {
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
