package com.whut.gulimall.seckill.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.whut.common.to.SeckillOrderTo;
import com.whut.common.utils.R;
import com.whut.common.vo.MemberResVo;
import com.whut.gulimall.seckill.constant.SeckillSessionConstant;
import com.whut.gulimall.seckill.feign.CouponFeignService;
import com.whut.gulimall.seckill.feign.ProductFeignService;
import com.whut.gulimall.seckill.interceptor.LoginUserInterceptor;
import com.whut.gulimall.seckill.service.SeckillService;
import com.whut.gulimall.seckill.to.SeckillSkuRedisTo;
import com.whut.gulimall.seckill.vo.SeckillSessionsWithSkusVo;
import com.whut.gulimall.seckill.vo.SkuInfoVo;
import org.apache.commons.lang.StringUtils;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;


import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SeckillServiceImpl implements SeckillService {

    @Autowired
    CouponFeignService couponFeignService;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    RedissonClient redissonClient;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Override
    public void uploadSeckillSkuLatest3Days() {
        // 扫描需要秒杀的活动
        R r = couponFeignService.getLatest3DaysSession();
        if (r.getCode() == 0) {
            // 上架商品List<SeckillSessionEntity>
            List<SeckillSessionsWithSkusVo> sessions = r.getData("data", new TypeReference<List<SeckillSessionsWithSkusVo>>() {
            });
            if (sessions != null && sessions.size() > 0) {
                // 缓存到redis
                // 1. 缓存活动信息seckill:sessions start_endTime->{sessionId_skuIds}
                saveSessionInfos(sessions);
                // 2. 缓存活动关联的商品信息(sessionId_skuId,skuInfos)
                saveSessionRelationSkuInfos(sessions);
            }

        }
    }

    @Override
    public List<SeckillSkuRedisTo> getCurrentSeckillSkus() {
        // 确定当前时间属于哪些场次
        // 1970
        long currentTime = new Date().getTime();
        Set<String> keys = redisTemplate.keys(SeckillSessionConstant.SESSIONS_CACHE_PREFIX + "*");
        for (String key : keys) {
            // seckill:sessions:1617206400000_1617213600000
            String replace = key.replace(SeckillSessionConstant.SESSIONS_CACHE_PREFIX, "");
            String[] s = replace.split("_");
            long startTime = Long.parseLong(s[0]);
            long endTime = Long.parseLong(s[1]);
            if (currentTime > startTime && currentTime < endTime) {
                // 获取这个秒杀场次所有的商品信息
                List<String> range = redisTemplate.opsForList().range(key, -100, 100);
                BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SeckillSessionConstant.SKUKILL_CACHE_PREFIX);
                List<String> list = hashOps.multiGet(range);
                if (list != null && list.size() > 0) {
                    List<SeckillSkuRedisTo> skuRedisTos = list.stream().map(item -> {
                        SeckillSkuRedisTo skuRedisTo = JSON.parseObject(item, SeckillSkuRedisTo.class);
//                        skuRedisTo.setRandomCode(null); // 当前秒杀开始了，需要随机码
                        return skuRedisTo;
                    }).collect(Collectors.toList());
                    return skuRedisTos;
                }
            }
        }

        return null;
    }

    @Override
    public SeckillSkuRedisTo getSkuSeckillInfo(Long skuId) {
        // 找到所有需要秒杀的商品key
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SeckillSessionConstant.SKUKILL_CACHE_PREFIX);
        Set<String> keys = hashOps.keys();
        if (keys != null && keys.size() > 0) {
            String regx = "\\d_" + skuId;
            for (String key : keys) {
                if (Pattern.matches(regx, key)) {
                    String json = hashOps.get(key);
                    SeckillSkuRedisTo seckillSkuRedisTo = JSON.parseObject(json, SeckillSkuRedisTo.class);
                    // 随机码
                    if (seckillSkuRedisTo != null) {
                        long time = new Date().getTime();
                        if (time < seckillSkuRedisTo.getStartTime() || time > seckillSkuRedisTo.getEndTime()) {
                            seckillSkuRedisTo.setRandomCode(null);
                        }
                        return seckillSkuRedisTo;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public String kill(String killId, String code, Integer num) {
        MemberResVo memberResVo = LoginUserInterceptor.loginUser.get();
        // 获取商品的详细信息
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SeckillSessionConstant.SKUKILL_CACHE_PREFIX);
        String json = hashOps.get(killId);
        if (!StringUtils.isEmpty(json)) {
            SeckillSkuRedisTo seckillSkuRedisTo = JSON.parseObject(json, SeckillSkuRedisTo.class);
            //校验合法性
            Long startTime = seckillSkuRedisTo.getStartTime();
            Long endTime = seckillSkuRedisTo.getEndTime();
            long time = new Date().getTime();
            // 1. 校验时间的合法性
            if (time >= startTime && time <= endTime) {
                // 2. 校验随机码和商品id
                String randomCode = seckillSkuRedisTo.getRandomCode();
                String redisKillId = seckillSkuRedisTo.getSeckillSkuRelationVo().getPromotionSessionId() + "_" + seckillSkuRedisTo.getSeckillSkuRelationVo().getSkuId();
                if (randomCode.equals(code) && redisKillId.equals(killId)) {
                    // 3. 验证购物数量
                    if (num <= seckillSkuRedisTo.getSeckillSkuRelationVo().getSeckillLimit()) {
                        // 4. 验证此人是否购买过,占位userId_SessionId_skuId
                        String redisKey = memberResVo.getId() + "_" + redisKillId;
                        // 自动过期
                        Boolean aBoolean = redisTemplate.opsForValue().setIfAbsent(redisKey, num.toString(), endTime - time, TimeUnit.MICROSECONDS);
                        if (aBoolean != null && aBoolean) {
                            // 说明该用户没买过该商品
                            RSemaphore semaphore = redissonClient.getSemaphore(SeckillSessionConstant.SKU_STOCK_SEMAPHORE + randomCode);
                            boolean b = semaphore.tryAcquire(num);
                            if (b) {
                                // 秒杀成功，快速下单，发送MQ消息
                                String orderSn = IdWorker.getTimeId();
                                SeckillOrderTo seckillOrder = new SeckillOrderTo();
                                seckillOrder.setOrderSn(orderSn);
                                seckillOrder.setMemberId(memberResVo.getId());
                                seckillOrder.setNum(num);
                                seckillOrder.setPromotionSessionId(seckillSkuRedisTo.getSeckillSkuRelationVo().getPromotionSessionId());
                                seckillOrder.setSkuId(seckillSkuRedisTo.getSeckillSkuRelationVo().getSkuId());
                                seckillOrder.setSeckillPrice(seckillSkuRedisTo.getSeckillSkuRelationVo().getSeckillPrice());
                                rabbitTemplate.convertAndSend("order-event-exchange", "order.seckill.order", seckillOrder);
                                return orderSn;
                            }
                            return null;
                        }
                    }
                }
            }
        }
        return null;
    }

    private void saveSessionInfos(List<SeckillSessionsWithSkusVo> sessions) {
        sessions.stream().forEach(session -> {
            long startTime = session.getStartTime().getTime();
            long endTime = session.getEndTime().getTime();
            String key = SeckillSessionConstant.SESSIONS_CACHE_PREFIX + startTime + "_" + endTime;
            Boolean hasKey = redisTemplate.hasKey(key);
            if (!hasKey) {
                List<String> skuIds = session.getRelationSkus().stream().map(item -> item.getPromotionSessionId().toString() + "_" + item.getSkuId().toString()).collect(Collectors.toList());
                // 缓存活动信息
                redisTemplate.opsForList().leftPushAll(key, skuIds);
            }
        });
    }

    private void saveSessionRelationSkuInfos(List<SeckillSessionsWithSkusVo> sessions) {

        sessions.stream().forEach(session -> {
            BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(SeckillSessionConstant.SKUKILL_CACHE_PREFIX);
            session.getRelationSkus().stream().forEach(seckillSkuRelationVo -> {
                if (!hashOps.hasKey(seckillSkuRelationVo.getPromotionSessionId().toString() + "_" + seckillSkuRelationVo.getSkuId().toString())) {
                    // 保存商品
                    SeckillSkuRedisTo redisTo = new SeckillSkuRedisTo();
                    // 1. Sku的基本信息
                    R r = productFeignService.getSkuInfo(seckillSkuRelationVo.getSkuId());
                    if (r.getCode() == 0) {
                        SkuInfoVo skuInfo = r.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                        });
                        redisTo.setSkuInfoVo(skuInfo);
                    }
                    // 2. sku秒杀信息
                    redisTo.setSeckillSkuRelationVo(seckillSkuRelationVo);
                    // 3. 设置当前商品的秒杀时间
                    redisTo.setStartTime(session.getStartTime().getTime());
                    redisTo.setEndTime(session.getEndTime().getTime());
                    // 4. 设置随机码
                    String token = UUID.randomUUID().toString().replace("-", "");
                    redisTo.setRandomCode(token);
                    hashOps.put(seckillSkuRelationVo.getPromotionSessionId().toString() + "_" + seckillSkuRelationVo.getSkuId().toString(), JSON.toJSONString(redisTo));
                    // 5. 使用库存作为分布式信号量 限流
                    RSemaphore semaphore = redissonClient.getSemaphore(SeckillSessionConstant.SKU_STOCK_SEMAPHORE + token);
                    // 商品可以秒杀的数量作为信号量
                    semaphore.trySetPermits(seckillSkuRelationVo.getSeckillCount());
                }
            });
        });
    }
}
