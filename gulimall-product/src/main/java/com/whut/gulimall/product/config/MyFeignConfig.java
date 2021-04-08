package com.whut.gulimall.product.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;


@Configuration
public class MyFeignConfig {

    @Bean("requestInterceptor")
    public RequestInterceptor requestInterceptor(){
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate requestTemplate) {
                // 拿到刚进来的请求
                ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (requestAttributes != null){
                    HttpServletRequest request = requestAttributes.getRequest(); // 老请求
                    if (request != null){
                        // 同步请求头数据，Cookie
                        String cookie = request.getHeader("Cookie");
                        // 给新请求同步老请求的cookie
                        requestTemplate.header("Cookie",cookie);
                    }
                }
            }
        };
    }
}
