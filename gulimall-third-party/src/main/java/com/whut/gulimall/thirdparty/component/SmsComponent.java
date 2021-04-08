package com.whut.gulimall.thirdparty.component;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import javax.mail.Transport;

@Component
@Data
public class SmsComponent {

    @Autowired
    private JavaMailSender mailSender;

    public void sendSmsCode(String phone, String code) {

    }

    public void sendMailCode(String code) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setSubject("验证码邮件");
        mailMessage.setText("您收到的验证码是：" + code);
        mailMessage.setFrom("1184384017@qq.com");
        mailMessage.setTo("1911801213@qq.com");
        mailSender.send(mailMessage);

    }
}
