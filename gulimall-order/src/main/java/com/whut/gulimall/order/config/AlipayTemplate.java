package com.whut.gulimall.order.config;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.whut.gulimall.order.vo.PayVo;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "alipay")
@Component
@Data
public class AlipayTemplate {

    //在支付宝创建的应用的id
    private   String app_id = "2021000117627832";

    // 商户私钥，您的PKCS8格式RSA2私钥
    private  String merchant_private_key = "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCMSQbs0VMQ+izw+b5uCUAK4r2f9LGQ7hABM0ZCNY1CQue3ja3uGwN4cWggbs+qFXt2b9yZcRHOBIGkyivBssV3OFuUqGLDAwyIDLSqQvImmgL5BsezVpMgr31GW3jSJZNcgSQavRTrdScWbcTE5L4PttHOsgMEZ37CDyL4Zo82yULhJJFr8mUfIl6w0NwzAv17hS9aKq/ryNa7vVY9PZ0ODnw/yFhQZ/Sc4a/mkXwAWaJibIyI+nZQ/MjGUL4nW8kwd8Te1iiCgcUQuzaKWAqhvHVTbOL4a/CPOf4lUaEQ2DSVdNZf6CpWcLC8EIZVZoowchlOi4Uec4tk+O12ZKiLAgMBAAECggEAf+2IdyVcTrsViJr24XlmAYdLzTZjRvsRdUbA51fWfXptvKFi3yObQpaIkir7DOuSLytLTFIkHm9VmmUTCgxwrouiWCpGBfgd0WNkzW9HS5Re6aEZM53bhY8C+sonn9vMSisqNYgAL8gh4P4w1iySddoN8iO7RKTsnyMWjNdZ+matAOjHeXPrx82+Wh4UZif5MvpHdILnQba2XE8BJw6u340z6wZApTEezowyoH1skbl8ljwbCQQLr0Vx3+314Vz9+yIgHcbYWIzvO+/cWGlU6JE7+GcrqeHNJLr8vN6iiPPk078BL8KzTf9tiMIrYpa9ZVeII5v0TcIorJ2m+aqrQQKBgQC//MfZ3El8FSkeWSYdpWQywxgjFduEDShGxbo2pGaQDzbTlJYjboAxpEBhXZjlFavSqISmghWJlcCaqplCfiN0BC+bJ9FLxI94rve4sreEcxheBUo/YzQ5+Hudjs6adCEszCd4tBBHhWxqpuv9GnekiR2Xx4oh0XhEJ1NO1xOTswKBgQC7DywssnqTFBxIAIEQQ7gv8/OxryoPWx1mmoKuJMHqi+PB/e1dA0ZgOqKASha9rXefpjLfKyL/mVJqS1mhzeit5IHA9SsunHGjvqa45vjT01vROEsGEKDELrSAdTkYS9lTPwxWkN/xaWU0AV8MWprfKNNKqvPIqJHXjJLXqh8LyQKBgGjLlVdBHEeD0W8EYH7cMaZXwcwc+TbKa8q90VhUQrut4lJ5j76FaaovKwk6quOZAbI6VDYDWbhBKuIB7yhRbA6+3jGF+YOvJlVMxGFoBC5jGNDxeEbDYtWl4evt8K50Z0tpoL4NI7m2hahttvDwSpBgW+vmemrdSRWClcG6OrBrAoGAD0pNNRYduNoC+cu4wQiYDKtnNxX3XSv4ekOU/QEaLL7kjh3ggiLbZNBGKjBkLXr5dT5TmDRhdv0kZHXmTLiVFWnBRXGE5xl4nIbf/+s5Wa9EKRTt5QAE9CRRMtLiKN9CU3Cq5ISRGZ2g3SBjkZHyaN1gcBELPlKxISpncmOAiCkCgYAt9fsBxqoOpk55iTgTiVBqXrGdUV6V8pi42t0h9EA+g+TFs+xOD1BzcHtqDDH20eaQ5kA9MH+s7KdPI+8kq5QE/12S/njJIc001d6C6EnDKLgcPbXIvoeXxtDjzXKIfuyDYrR0zTuE/KWEY82F3LVPfBrzV71BH5tUZy5BAts8lA==";
    // 支付宝公钥,查看地址：https://openhome.alipay.com/platform/keyManage.htm 对应APPID下的支付宝公钥。
    private  String alipay_public_key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA648OOTkyNss8E0HovaoTr4XkHGul+0Dowc2k3fj+2jnz2Mj8YaQ/HpwANg2h1fe6Tfe+K76MM0bF188QyY8rwajmYepw7Y6DgJS13PB568VwsgZ/Ihh98ClHN1hB26Fw02YXb8HX+m/ypd8dNg+Ohk1B4RLsWkXm79Vht9ez5i9J93PKO0IRZX8DGmknQNxiQsp0L3JBWRC0Aj1P5Cgbt7XKpFUxEitIEMpja1/xsXle5XKFItCXuV/OoS6QcATBYplL9o1b7VzlB5eRuc4/9fkXc+sOpUcl5U2Qa25E9ufbzceeF7cedbda5RdkCWX+WLaY4LIhSRjiVAwP4cRQwQIDAQAB";
    // 服务器[异步通知]页面路径  需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    // 支付宝会悄悄的给我们发送一个请求，告诉我们支付成功的信息
    private  String notify_url = "https://f8ed09648008.ngrok.io/payed/notify";

    // 页面跳转同步通知页面路径 需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    //同步通知，支付成功，一般跳转到成功页
    private  String return_url = "http://member.gulimall.com/memberOrderList.html";

    // 签名方式
    private  String sign_type = "RSA2";

    // 字符编码格式
    private  String charset = "utf-8";

    // 相对收单时间
    private String outTime = "1m";

    // 支付宝网关； https://openapi.alipaydev.com/gateway.do
    private  String gatewayUrl = "https://openapi.alipaydev.com/gateway.do";

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
                + "\"timeout_express\":\""+outTime+"\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");

        String result = alipayClient.pageExecute(alipayRequest).getBody();

        //会收到支付宝的响应，响应的是一个页面，只要浏览器显示这个页面，就会自动来到支付宝的收银台页面
        System.out.println("支付宝的响应："+result);

        return result;

    }
}
