package com.acc.somsomparty.domain.Queue.service;

import com.acc.somsomparty.domain.Queue.kafka.QueueProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class QueueService {
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final QueueProducer queueProducer;
    // 사용자 대기 queue의 key
    private final String QUEUE_WAIT_KEY = "queue:wait:%s";

    // 유저 대기열 등록
    // redis의 sorted set을 대기열로 사용 (key: userId , value: unix timestamp)
    public Mono<Void> registerWaitQueue(String festivalId, String userId) {
        return Mono.fromRunnable(() -> queueProducer.enqueueUser(festivalId, userId));
    }

    // 대기열에서 사용자 순위 조회
    public Mono<Long> getRank(String festivalId, String userId) {
        return reactiveRedisTemplate.opsForZSet()
                .rank(QUEUE_WAIT_KEY.formatted(festivalId), userId)
                .defaultIfEmpty(-1L) // 대기열에 없다면 -1을 return
                .map(rank -> rank >= 0 ? rank + 1 : -1L);
    }

    // 페이지 이탈 시 대기열에서 삭제
    public Mono<Void> removeUserFromQueue(String festivalId, String userId) {
        return reactiveRedisTemplate.opsForZSet()
                .remove(QUEUE_WAIT_KEY.formatted(festivalId), userId)
                .flatMap(count -> {
                    if (count > 0) {
                        log.info("사용자가 대기열에서 제거 [Festival: {}, User: {}]", festivalId, userId);
                    } else {
                        log.warn("대기열에 사용자 없음 [Festival: {}, User: {}]", festivalId, userId);
                    }
                    return Mono.empty();
                });
    }

    // 대기 중인 사람이 없는지 확인
    public Mono<Boolean> isQueueEmpty(String festivalId) {
        String key = QUEUE_WAIT_KEY.formatted(festivalId);
        return reactiveRedisTemplate.opsForZSet()
                .size(key)
                .defaultIfEmpty(0L)
                .doOnNext(size -> log.info("Redis ZSet size for key {}: {}", key, size))
                .map(queueSize -> queueSize == 0);
    }
}
