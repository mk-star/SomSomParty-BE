package com.acc.somsomparty.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.*;

import java.time.Duration;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class RedisConfig {
    private final ObjectMapper objectMapper;
    private final RedisClusterProperties redisClusterProperties;


    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory());

        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());

        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
        serializer.setObjectMapper(objectMapper);
        redisTemplate.setValueSerializer(serializer);
        redisTemplate.setHashValueSerializer(serializer);

        return redisTemplate;
    }

//    @Bean
//    @Primary
//    public RedisConnectionFactory redisConnectionFactory() {
//        return new LettuceConnectionFactory("localhost", 8080);
//    }


    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        final List<RedisNode> redisNodes = redisClusterProperties.getNodes().stream()
                .map(node -> new RedisNode(node.split(":")[0], Integer.parseInt(node.split(":")[1])))
                .toList();

        // Cluster 설정
        RedisClusterConfiguration clusterConfiguration = new RedisClusterConfiguration();
        clusterConfiguration.setClusterNodes(redisNodes);
        clusterConfiguration.setMaxRedirects(redisClusterProperties.getMaxRedirects());

        // Socket 옵션
        SocketOptions socketOptions = SocketOptions.builder()
                .connectTimeout(Duration.ofMillis(100L))
                .keepAlive(true)
                .build();

        // Cluster Topology refresh 옵션
        ClusterTopologyRefreshOptions clusterTopologyRefreshOptions = ClusterTopologyRefreshOptions.builder()
                .dynamicRefreshSources(true)
                .enableAllAdaptiveRefreshTriggers()
                .enablePeriodicRefresh(Duration.ofMinutes(30L))
                .build();

        // Cluster Client 옵션
        ClientOptions clientOptions = ClusterClientOptions.builder()
                .topologyRefreshOptions(clusterTopologyRefreshOptions)
                .socketOptions(socketOptions)
                .build();

        // Lettuce Client 설정
        LettuceClientConfiguration clientConfiguration = LettuceClientConfiguration.builder()
                .clientOptions(clientOptions)
                .commandTimeout(Duration.ofMillis(3000L))
                .build();

        return new LettuceConnectionFactory(clusterConfiguration, clientConfiguration);
    }
}
