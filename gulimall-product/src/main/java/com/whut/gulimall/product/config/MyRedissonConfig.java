package com.whut.gulimall.product.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class MyRedissonConfig {

    @Bean(destroyMethod="shutdown")
    RedissonClient redisson() throws IOException {
        // 1. 创建配置
        Config config = new Config();
        //可以用"rediss://"来启用SSL连接
        config.useSingleServer().setAddress("redis://10.138.213.16:6379");
        // 2. 创建RedissonClient实例
        return Redisson.create(config);
    }

}
