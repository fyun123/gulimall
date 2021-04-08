package com.whut.gulimall.seckill.to;

import com.whut.gulimall.seckill.vo.SeckillSkuRelationVo;
import com.whut.gulimall.seckill.vo.SkuInfoVo;
import lombok.Data;

@Data
public class SeckillSkuRedisTo {

    /**
     * 秒杀商品的秒杀信息
     */
    private SeckillSkuRelationVo seckillSkuRelationVo;

    /**
     * 秒杀商品的详细信息
     */
    private SkuInfoVo skuInfoVo;

    /**
     * 秒杀活动的起始时间
     */
    private Long startTime;

    /**
     * 秒杀活动的结束时间
     */
    private Long endTime;

    /**
     * 商品秒杀随机码
     */
    private String randomCode;

}
