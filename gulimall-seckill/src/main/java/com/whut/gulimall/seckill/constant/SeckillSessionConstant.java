package com.whut.gulimall.seckill.constant;


public class SeckillSessionConstant {
    //redis缓存秒杀活动信息
    public static final String SESSIONS_CACHE_PREFIX = "seckill:sessions:";
    // 秒杀商品信息
    public static final String SKUKILL_CACHE_PREFIX = "seckill:skus";
    // 库存信号量
    public static final String SKU_STOCK_SEMAPHORE = "seckill:stock:";

    // 分布式锁
    public static final String UPLOAD_LOCK = "seckill:upload:lock";
}
