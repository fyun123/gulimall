package com.whut.gulimall.product.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.concurrent.ExecutionException;

@FeignClient("gulimall-cart")
public interface CartFeignService {

    @GetMapping("/getCountNum")
    Integer getCountNum() throws ExecutionException, InterruptedException;

}
