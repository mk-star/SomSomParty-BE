package com.acc.somsomparty.global.config;

import lombok.RequiredArgsConstructor;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//@Configuration
//@RequiredArgsConstructor
//public class RedissonConfig {
//
//    private static final String REDISSON_PREFIX = "redis://";
//
//    private final RedisClusterProperties redisClusterProperties;
//
//    @Bean
//    public RedissonClient redissonClient() {
//        Config config = new Config();
//        ClusterServersConfig csc = config.useClusterServers()
//                .setScanInterval(2000)
//                .setConnectTimeout(100)
//                .setTimeout(3000)
//                .setRetryAttempts(3)
//                .setRetryInterval(1500);
//        redisClusterProperties.getNodes().forEach(node -> csc.addNodeAddress(REDISSON_PREFIX + node));
//        return Redisson.create(config);
//    }
//}