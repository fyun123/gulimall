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
import org.checkerframework.checker.units.qual.A;
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
    private OSSClient ossClient;


    @Test
    public void testUpload() throws FileNotFoundException {
        // 上传文件流。
        InputStream inputStream = new FileInputStream("F:\\图片\\logo.png");
        ossClient.putObject("gulimall-fyun", "logo44.png", inputStream);
        // 关闭OSSClient。
        ossClient.shutdown();
        System.out.println("上传成功");
    }



}
