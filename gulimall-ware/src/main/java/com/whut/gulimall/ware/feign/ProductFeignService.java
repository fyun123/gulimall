package com.whut.gulimall.ware.feign;

import com.whut.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient("gulimall-gateway")
public interface ProductFeignService {

    /**
     *  1. 所有请求过网关
     *      1）. @FeignClient("gulimall-gateway")
     *      2）. /api/product/skuinfo/info/{skuId}
     *  2. 直接让后台指定服务处理
     *      1）.@FeignClient("gulimall-product")
     *      2). /product/skuinfo/info/{skuId}
     * @param skuId
     * @return
     */
    @RequestMapping("/api/product/skuinfo/info/{skuId}")
    public R info(@PathVariable("skuId") Long skuId);

}
