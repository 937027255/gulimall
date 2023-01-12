package com.atguigu.gulimail.order.config;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author zr
 * @date 2021/12/31 10:26
 */
@ConfigurationProperties(prefix = "alipay")
@Component
@Data
public class AlipayTemplate {

    // 应用ID,您的APPID，收款账号既是您的APPID对应支付宝账号
    public String app_id = "2021000121660026";

    // 商户私钥，您的PKCS8格式RSA2私钥
    public String merchant_private_key = "MIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAoIBAQCQPEn6tbGLN2kn4onQRLXLXO4u6+047zN4jK6JJqp/wIFJciSRBVhHizrcAGYj8jSeemBkImGplKMTy4KAY1Xy3wmVHQEbl0NP305VGpFRHA76GpFMUin9yYMChd/qB+nTt+osVjxpjXYKlMHiymR6WDpmH64d+deqG3BJ40kdNnaENTe5M/TzORY/O0F4BR0umicR/0xiurPzMHcNzEJZQXQ9zp8p19QCdckfKqfGq21xGOtgyQFgOzUwQ4iOcO24bgdQ0C58hiBsgMlPSnwEXSNE4CzQB+/yFZWwBu2QDxVbL74WRDrZhXlES6x+FWUKBXttu60QEaovNqqpNV0lAgMBAAECggEBAI32SPoyCuVzTFSta+dJOYVHmkckfwAirw5fqQnkvLuwxY98gPEbyZ/hMbWHzwBiS9gPqv/jtywp17iL/Y5QNzAsxkm1mnmFkAL1TywEUjL64uLXc1sjI6FezUuWaKT31PVFd37cAeoCCcSwVYTan7qrTA9n7wAiAFq1VvfidPwCOt8S8diiQgb9odmTzW0b6eyW3PR51bj4If0YKVwIkuIt8WyrAZcoxlhLw5z+k5ZtAmcqOWJa1BLyabkme0ygY1rHWTZWDxguI1AInDJ5Q8RCDelFYvfzofUtuWk6WOLsuQczOKGzsyHJBdHsXiIKIEUVPodMTR87rl9M1wynawECgYEA0/T66vmBbcl1k3bJbIcm0l2o0oU+QPfMIbaa/rq7YZn9LYGH21FXFNXwOheGGUM49OmNiKn7msszb+j2j0ZbiMCZLSS+161H2oiXwO4/n7OsOaAb2xJinT1d1sfwAG8+MMadE/mBFBdJ7Q99uM4eYLajbN1qZyG8gujT57+va80CgYEArjTgQNQQh3GloX0EG5zaQ4sfE2rZlB83fTqrmxsVQ4KnGYNc5tNZm+U8EuJsUz+IFtmZiz2YkzMelI9XLflke9DErPgo0AGT88rHzjNtg8Y89AM934PVq7Knv9TdrDXYDX2RrlDIh5sp5XGY1X/c2IOOvxt3YyifVLKDcEr4TrkCgYA8leEHO2yIojZOgXzP3c32AbmY+2cxEOOzm0uwllWrb2XUFRttUFHQ7dot8L1vCS0FrKC9OMFnnL/GNFQN3jGZ5FukcMVwRuKsD7E4/6EGGEiZbC+qLhYsFfd1xk7PAP1I2ezaYc5InOOPtIbeRWSfhnUuUBfzBMyqqryvmLYe3QKBgQCfQzH9sqKF4S1mjM+TmvukIm0/qEDQfRWSxG+cts7i0HCKD6GAWZqjvpgEP8J+/ScilL8eICr78BE2CjHOkyD9XShhZeKR/89OY4X1iujFPxf8kXYggPt4JDXeV/Js/TDPbIwiDDK4Xu+Xlxn9ng6+Vs/LAYZNPhckLKujnraR6QKBgQCTSbZNRZlmaLB1b68aDs2pm9z/3LElKLeqLxBI9Erhdm1eT/LxkY8E7TbBx3LOIEhSjDZ1rOMrETBM0xMKXUSzr0RELPQ3KxHt/IyOoyIsUd2n0qzBpHVFVu5buMCkaJT61mA3L6tFr95V5cZo5lKyaX81OPZ9phKWDef3D7TcEw==";

    // 支付宝公钥,查看地址：https://openhome.alipay.com/platform/keyManage.htm 对应APPID下的支付宝公钥。
    public String alipay_public_key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqvCsCgRxBXWiOOUxPFYJwn+5Z+FJTpYH1ev+DWBNVLaCtqlEcH8itKYB5vSR1g6tbXntirJaJkgxXIzq4GkaJjl+Kiry2uDlW0f8gRhJ1I36OWsjD1zj/jq4EuNwLbyu/avLMYo9HyOz0MPnomj0qiDWjArxuAnZhPzmYuL0VbUU8PxakZfGzVwJt8PdJIjuMrA9gJQ4IkISOcXfIql6vkhvIqxh0vsGSzo764yCfLHzjnH4EhARvbt+XnJESprdV7MxtMr+YC8dVkq+U2hqWwFZhsiyCXzEwNfMyB+iJHtWPaL00nIiiHIkbiqIVVLRJ2uKBXaZTpbf0fq5BfYWNQIDAQAB";

    // 服务器[异步通知]页面路径  需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    // 支付宝会悄悄的给我们发送一个请求，告诉我们支付成功的信息
    public String notify_url = "http://urk7jn.natappfree.cc/payed/notify";

    // 页面跳转同步通知页面路径 需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    //同步通知，支付成功，一般跳转到成功页
    public String return_url = "http://member.gulimail.com/memberOrder.html";

    // 签名方式
    private  String sign_type = "RSA2";

    // 字符编码格式
    private  String charset ="utf-8";

    //订单超时时间
    private String timeout = "1m";

    // 支付宝网关； https://openapi.alipaydev.com/gateway.do
    public String gatewayUrl = "https://openapi.alipaydev.com/gateway.do";

    public  String pay(PayVo vo) throws AlipayApiException {
        //AlipayClient alipayClient = new DefaultAlipayClient(AlipayTemplate.gatewayUrl, AlipayTemplate.app_id, AlipayTemplate.merchant_private_key, "json", AlipayTemplate.charset, AlipayTemplate.alipay_public_key, AlipayTemplate.sign_type);
        //1、根据支付宝的配置生成一个支付客户端
        AlipayClient alipayClient = new DefaultAlipayClient(gatewayUrl,
                app_id, merchant_private_key, "json",
                charset, alipay_public_key, sign_type);

        //2、创建一个支付请求 //设置请求参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(return_url);
        alipayRequest.setNotifyUrl(notify_url);

        //商户订单号，商户网站订单系统中唯一订单号，必填
        String out_trade_no = vo.getOut_trade_no();
        //付款金额，必填
        String total_amount = vo.getTotal_amount();
        //订单名称，必填
        String subject = vo.getSubject();
        //商品描述，可空
        String body = vo.getBody();

        alipayRequest.setBizContent("{\"out_trade_no\":\""+ out_trade_no +"\","
                + "\"total_amount\":\""+ total_amount +"\","
                + "\"subject\":\""+ subject +"\","
                + "\"body\":\""+ body +"\","
                + "\"timeout_express\":\""+timeout+"\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");

        String result = alipayClient.pageExecute(alipayRequest).getBody();

        //会收到支付宝的响应，响应的是一个页面，只要浏览器显示这个页面，就会自动来到支付宝的收银台页面
        System.out.println("支付宝响应：登录页面的代码\n"+result);

        return result;
    }

    @Data
    public static class PayVo {

        private String out_trade_no; // 商户订单号 必填
        private String subject; // 订单名称 必填
        private String total_amount;  // 付款金额 必填
        private String body; // 商品描述 可空
    }
}
