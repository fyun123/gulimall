package com.whut.gulimall.ware.feign;

import com.whut.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
@FeignClient("gulimall-order")
public interface OrderFeignService {

    @GetMapping("/order/omsorder/status/{orderSn}")
    R getOrderStatus(@PathVariable("orderSn") String orderSn);
}
