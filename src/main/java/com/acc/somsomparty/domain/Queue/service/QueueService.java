package com.acc.somsomparty.domain.Queue.service;

import com.acc.somsomparty.domain.Queue.kafka.QueueProducer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class QueueService {
    private final RedisTemplate<String, String> redisTemplate;
    private final QueueProducer queueProducer;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    // 사용자 대기 queue의 key
    private final String QUEUE_WAIT_KEY = "queue:wait:{%s}";

    // 큐 초기화 + TTL
    public void initQueue(String festivalId) {
        String queueKey = QUEUE_WAIT_KEY.formatted(festivalId);
        Boolean exists = redisTemplate.hasKey(queueKey);
        if (Boolean.FALSE.equals(exists)) {
            log.info("대기열 초기화 [Festival: {}]", festivalId);
            redisTemplate.expire(queueKey, Duration.ofHours(5));
        }
    }

    // 재접속시?
    // 유저 대기열 등록
    // redis의 sorted set을 대기열로 사용 (key: userId , value: unix timestamp)
    public void joinQueue(String festivalId, String userId) {
        String queueKey = QUEUE_WAIT_KEY.formatted(festivalId);
        long timestamp = Instant.now().toEpochMilli();

        Boolean added = redisTemplate.opsForZSet().add(queueKey, userId, timestamp);
        if (Boolean.TRUE.equals(added)) {
            log.info("대기열 등록 [Festival: {}, User: {}]", festivalId, userId);
        } else {
            log.warn("이미 등록된 사용자 [Festival: {}, User: {}]", festivalId, userId);
        }

        // Kafka에 전송
        queueProducer.enqueueUser(festivalId, userId, timestamp);
    }


    // 대기열을 나가는 사용자에게 WebSocket 메시지 전송
    public void sendRank(String festivalId, String userId) {
        String message = buildMessage(festivalId, userId, -1);
        messagingTemplate.convertAndSend("/topic/queue.status." + festivalId + "." + userId, message);
    }

    // 대기열에 있는 각 사용자에게 개별 WebSocket 메시지 전송
    public void sendRanks(String festivalId) {
        String queueKey = QUEUE_WAIT_KEY.formatted(festivalId);
        Long size = redisTemplate.opsForZSet().size(queueKey);
        if (size == null || size == 0) return;

        Set<String> users = redisTemplate.opsForZSet().range(queueKey, 0, size - 1);
        if (users == null) return;

        for (String userId : users) {
            Long rank = redisTemplate.opsForZSet().rank(queueKey, userId);
            if (rank == null) rank = -1L;
            String message = buildMessage(festivalId, userId, rank);
            messagingTemplate.convertAndSend("/topic/queue.status." + festivalId + "." + userId, message);
        }
    }

    private String buildMessage(String festivalId, String userId, long rank) {
        Map<String, Object> payload = Map.of(
                "festivalId", festivalId,
                "userId", userId,
                "rank", rank
        );
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("메시지 생성 실패", e);
            return "{}";
        }
    }

    // 페이지 이탈 시 대기열에서 삭제
    public void removeUserFromQueue(String festivalId, String userId) {
        Long removed = redisTemplate.opsForZSet().remove(QUEUE_WAIT_KEY.formatted(festivalId), userId);
        if (removed != null && removed > 0) {
            log.info("사용자가 대기열에서 제거 [Festival: {}, User: {}]", festivalId, userId);
        } else {
            log.warn("대기열에 사용자 없음 [Festival: {}, User: {}]", festivalId, userId);
        }
    }

//    // 대기 중인 사람이 없는지 확인
//    public Mono<Boolean> isQueueEmpty(String festivalId) {
//        String key = QUEUE_WAIT_KEY.formatted(festivalId);
//        return reactiveRedisTemplate.opsForZSet()
//                .size(key)
//                .defaultIfEmpty(0L)
//                .doOnNext(size -> log.info("Redis ZSet size for key {}: {}", key, size))
//                .map(queueSize -> queueSize == 0);
//    }

    // 대기 중인 사람이 없는지 확인
//    public Mono<Long> getQueueSize(String festivalId) {
//        String key = QUEUE_WAIT_KEY.formatted(festivalId);
//        return reactiveRedisTemplate.opsForZSet()
//                .size(key)
//                .defaultIfEmpty(0L)
//                .doOnNext(size -> log.info("Redis ZSet size for key {}: {}", key, size));
//    }

    // 대기열에 유저가 있는지 확인
//    public Mono<Boolean> exists(String festivalId, String userId) {
//        return reactiveRedisTemplate.opsForZSet()
//                .score(QUEUE_WAIT_KEY.formatted(festivalId), userId)      // userId의 score 조회
//                .map(Objects::nonNull)   // score가 있으면 존재
//                .defaultIfEmpty(false) ;   // 없으면 false
//                //.doOnNext(exists -> log.warn("대기열에 사용자 존재 여부 [Festival: {}, User: {}] : {}", festivalId, userId, exists));
//    }
}
