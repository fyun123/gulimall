package com.whut.gulimall.seckill.service;

import com.whut.gulimall.seckill.to.SeckillSkuRedisTo;

import java.util.List;

public interface SeckillService {
    /**
     * 上架最近三天的秒杀商品
     */
    void uploadSeckillSkuLatest3Days();

    /**
     * 返回当前时间可以参与秒杀的商品
     * @return
     */
    List<SeckillSkuRedisTo> getCurrentSeckillSkus();

    /**
     * 查询当前商品的秒杀信息
     * @param skuId
     * @return
     */
    SeckillSkuRedisTo getSkuSeckillInfo(Long skuId);

    String kill(String killId, String code, Integer num);
}
