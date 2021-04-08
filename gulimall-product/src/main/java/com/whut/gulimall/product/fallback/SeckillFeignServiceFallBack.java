package com.whut.gulimall.product.fallback;

import com.whut.common.exception.BizCodeEnume;
import com.whut.common.utils.R;
import com.whut.gulimall.product.feign.SeckillFeignService;
import org.springframework.stereotype.Component;

@Component
public class SeckillFeignServiceFallBack implements SeckillFeignService {
    @Override
    public R getSkuSeckillInfo(Long skuId) {
        return R.error(BizCodeEnume.TOO_MANY_REQUEST.getCode(),BizCodeEnume.TOO_MANY_REQUEST.getMsg());
    }
}
