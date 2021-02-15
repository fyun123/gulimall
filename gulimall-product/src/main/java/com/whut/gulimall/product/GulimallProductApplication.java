package com.whut.gulimall.product;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * JSR303
 *      1). 导入依赖
 *          <dependency>
 *             <groupId>org.springframework.boot</groupId>
 *             <artifactId>spring-boot-starter-validation</artifactId>
 *         </dependency>
 *      2). 在需校验的属性上标注@NotNull、@URL等
 *      3). 在controller里面加上@Valid，并在校验后面紧跟参数BindingResult，用来接收校验信息
 *
 * 统一异常处理@ControllerAdvice
 *      1).
 */
@EnableFeignClients(basePackages = "com.whut.gulimall.product.feign")
@MapperScan("com.whut.gulimall.product.dao")
@SpringBootApplication
@EnableDiscoveryClient
public class GulimallProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallProductApplication.class, args);
    }

}
