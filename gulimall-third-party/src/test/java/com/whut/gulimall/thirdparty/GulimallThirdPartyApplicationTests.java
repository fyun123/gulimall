package com.whut.gulimall.thirdparty;

import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyun.oss.OSSClient;
import com.google.gson.Gson;
import com.whut.gulimall.thirdparty.component.SmsComponent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;

@SpringBootTest
class GulimallThirdPartyApplicationTests {

    @Autowired
    SmsComponent smsComponent;

    @Resource
    private OSSClient ossClient;
    // 设置公共请求参数，初始化Client。
    private DefaultProfile profile = DefaultProfile.getProfile(
            "your-regionId",// API支持的地域ID，如短信API的值为：cn-hangzhou。
            "LTAI4G5ybdsccYBdEGsPfRVT",// 您的AccessKey ID。
            "RoCvpXoNLGsrZ8V0BZfopNCzxVeyp7");// 您的AccessKey Secret。
    private IAcsClient client = new DefaultAcsClient(profile);

    @Test
    public void sendSms() throws Exception {
//        DefaultProfile profile = DefaultProfile.getProfile("cn-hangzhou", "LTAI4G5ybdsccYBdEGsPfRVT", "RoCvpXoNLGsrZ8V0BZfopNCzxVeyp7");
//        IAcsClient client = new DefaultAcsClient(profile);
//
//        CommonRequest request = new CommonRequest();
//        request.setSysMethod(MethodType.POST);
//        request.setSysDomain("dysmsapi.aliyuncs.com");
//        request.setSysVersion("2017-05-25");
//        request.setSysAction("SendSms");
//        request.putQueryParameter("SignName", "阿里云通信");
//        request.putQueryParameter("RegionId", "cn-hangzhou");
//        request.putQueryParameter("PhoneNumbers", "15872357910");
//        try {
//            CommonResponse response = client.getCommonResponse(request);
//            System.out.println(response.getData());
//        } catch (ServerException e) {
//            e.printStackTrace();
//        } catch (ClientException e) {
//            e.printStackTrace();
//        }
        try {
            // 创建短信模板
            String templateCode = addSmsTemplate();
            // 使用刚创建的短信模板发送短信
            String bizId = sendSms(templateCode);
            // 根据短信发送流水号查询短信发送情况
            querySendDetails(bizId);
        } catch (ClientException e) {
            e.printStackTrace();
        }
    }
    private static void log_print(String functionName, Object result) {
        Gson gson = new Gson();
        System.out.println("-------------------------------" + functionName + "-------------------------------");
        System.out.println(gson.toJson(result));
    }
    /**
     * 添加短信模板
     */
    private String addSmsTemplate() throws ClientException {
        CommonRequest addSmsTemplateRequest = new CommonRequest();
        addSmsTemplateRequest.setSysDomain("dysmsapi.aliyuncs.com");
        addSmsTemplateRequest.setSysAction("AddSmsTemplate");
        addSmsTemplateRequest.setSysVersion("2017-05-25");
        // 短信类型。0：验证码；1：短信通知；2：推广短信；3：国际/港澳台消息
        addSmsTemplateRequest.putQueryParameter("TemplateType", "0");
        // 模板名称，长度为1~30个字符
        addSmsTemplateRequest.putQueryParameter("TemplateName", "测试短信模板");
        // 模板内容，长度为1~500个字符
        addSmsTemplateRequest.putQueryParameter("TemplateContent", "您正在申请手机注册，验证码为：${code}，5分钟内有效！");
        // 短信模板申请说明
        addSmsTemplateRequest.putQueryParameter("Remark", "测试");
        CommonResponse addSmsTemplateResponse = client.getCommonResponse(addSmsTemplateRequest);
        String data = addSmsTemplateResponse.getData();
        // 消除返回文本中的反转义字符
        String sData = data.replaceAll("'\'", "");
        log_print("addSmsTemplate", sData);
        Gson gson = new Gson();
        // 将字符串转换为Map类型，取TemplateCode字段值
        Map map = gson.fromJson(sData, Map.class);
        Object templateCode = map.get("TemplateCode");
        return templateCode.toString();
    }

    /**
     * 发送短信
     */
    private String sendSms(String templateCode) throws ClientException {
        CommonRequest request = new CommonRequest();
        request.setSysDomain("dysmsapi.aliyuncs.com");
        request.setSysVersion("2017-05-25");
        request.setSysAction("SendSms");
        // 接收短信的手机号码
        request.putQueryParameter("PhoneNumbers", "15872357910");
        // 短信签名名称。请在控制台签名管理页面签名名称一列查看（必须是已添加、并通过审核的短信签名）。
        request.putQueryParameter("SignName", "阿里云通信");
        // 短信模板ID
        request.putQueryParameter("TemplateCode", templateCode);
        // 短信模板变量对应的实际值，JSON格式。
        request.putQueryParameter("TemplateParam", "{\"code\":\"8888\"}");
        CommonResponse commonResponse = client.getCommonResponse(request);
        String data = commonResponse.getData();
        String sData = data.replaceAll("'\'", "");
        log_print("sendSms", sData);
        Gson gson = new Gson();
        Map map = gson.fromJson(sData, Map.class);
        Object bizId = map.get("BizId");
        return bizId.toString();
    }

    /**
     * 查询发送详情
     */
    private void querySendDetails(String bizId) throws ClientException {
        CommonRequest request = new CommonRequest();
        request.setSysDomain("dysmsapi.aliyuncs.com");
        request.setSysVersion("2017-05-25");
        request.setSysAction("QuerySendDetails");
        // 接收短信的手机号码
        request.putQueryParameter("PhoneNumber", "156xxxxxxxx");
        // 短信发送日期，支持查询最近30天的记录。格式为yyyyMMdd，例如20191010。
        request.putQueryParameter("SendDate", "20191010");
        // 分页记录数量
        request.putQueryParameter("PageSize", "10");
        // 分页当前页码
        request.putQueryParameter("CurrentPage", "1");
        // 发送回执ID，即发送流水号。
        request.putQueryParameter("BizId", bizId);
        CommonResponse response = client.getCommonResponse(request);
        log_print("querySendDetails", response.getData());
    }



    @Test
    public void testUpload() throws FileNotFoundException {
        // 上传文件流。
        InputStream inputStream = new FileInputStream("F:\\图片\\logo.png");
        ossClient.putObject("gulimall-fyun", "logo44.png", inputStream);
        // 关闭OSSClient。
        ossClient.shutdown();
        System.out.println("上传成功");
    }

    @Test
    public void testSendMailCode(){
        smsComponent.sendMailCode("123asd");
    }

}
