package com.whut.gulimall.product.vo;


import lombok.Data;

@Data
public class SeckillInfoVo {

    /**
     * 秒杀商品的秒杀信息
     */
    private SeckillSkuRelationVo seckillSkuRelationVo;

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
