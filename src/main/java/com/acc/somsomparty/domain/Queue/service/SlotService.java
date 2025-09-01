package com.acc.somsomparty.domain.Queue.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlotService {
    private final String SLOT_ACTIVE_KEY = "slots:active:%s";
    private final String SLOT_MAX_KEY = "slots:max:%s";
    private final String QUEUE_WAIT_KEY = "queue:wait:%s";
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    // 슬롯 초기화
    public Mono<Void> initializeSlot(String festivalId, int maxSlot) {
        String activeKey = SLOT_ACTIVE_KEY.formatted(festivalId);
        String maxKey = SLOT_MAX_KEY.formatted(festivalId);

        return reactiveRedisTemplate.opsForValue()
                .set(activeKey, "0") // 활성 슬롯 초기화
                .then(reactiveRedisTemplate.opsForValue().set(maxKey, String.valueOf(maxSlot)))
                .then(); // 최대 슬롯 설정
    }

//    public Mono<Boolean> hasAvailableSlot(String festivalId) {
//        String activeKey = SLOT_ACTIVE_KEY.formatted(festivalId);
//        String maxKey = SLOT_MAX_KEY.formatted(festivalId);
//
//        Mono<Integer> activeMono = reactiveRedisTemplate.opsForValue()
//                .get(activeKey)
//                .defaultIfEmpty("0")
//                .map(Integer::parseInt);
//
//        Mono<Integer> maxMono = reactiveRedisTemplate.opsForValue()
//                .get(maxKey)
//                .defaultIfEmpty("0")
//                .map(Integer::parseInt);
//
//        return Mono.zip(activeMono, maxMono)
//                .map(tuple -> {
//                    int active = tuple.getT1();
//                    int max = tuple.getT2();
//                    return active < max;
//                });
//    }

    // 저장 가능한 슬롯의 개수가 몇 개인지 반환
//    public Mono<Long> getAvailableSlots(String festivalId) {
//        String activeKey = SLOT_ACTIVE_KEY.formatted(festivalId);
//        String maxKey = SLOT_MAX_KEY.formatted(festivalId);
//        String queueKey = QUEUE_WAIT_KEY.formatted(festivalId);
//
//        Mono<Integer> activeMono = reactiveRedisTemplate.opsForValue()
//                .get(activeKey)
//                .defaultIfEmpty("0")
//                .map(Integer::parseInt);
//
//        Mono<Integer> maxMono = reactiveRedisTemplate.opsForValue()
//                .get(maxKey)
//                .defaultIfEmpty("0")
//                .map(Integer::parseInt);
//
//        Mono<Long> queueMono = reactiveRedisTemplate.opsForZSet()
//                .size(queueKey) // zset의 크기를 반환
//                .defaultIfEmpty(0L);
//
//        return Mono.zip(activeMono, maxMono, queueMono)
//                .map(tuple -> {
//                    int active = tuple.getT1();
//                    int max = tuple.getT2();
//                    long size = tuple.getT3();
//                    long availableSlots = max - active;
//                    return Math.min(availableSlots, size); // 최소값 반환
//                });
//    }

    // 최대 슬롯 수 확인하고 점유 가능하면 active 슬롯 증가하고 pop한 사용자 ID 반환
    public Mono<String> acquireSlotAndPopUser(String festivalId) {
        String queueKey = QUEUE_WAIT_KEY.formatted(festivalId);
        String activeKey = SLOT_ACTIVE_KEY.formatted(festivalId);
        String maxKey = SLOT_MAX_KEY.formatted(festivalId);

        DefaultRedisScript<String> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/acquire_and_pop.lua")));
        script.setResultType(String.class);

        return reactiveRedisTemplate.execute(script, List.of(queueKey, activeKey, maxKey))
                .singleOrEmpty()
                .defaultIfEmpty("");
    }

//    // 유저가 대기열에서 나가면 끝나면 슬롯을 해제함
//    public Mono<Void> releaseSlot(String festivalId, String userId) {
//        String activeKey = SLOT_ACTIVE_KEY.formatted(festivalId);
//
//        DefaultRedisScript<Boolean> script = new DefaultRedisScript<>();
//        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/release_slot.lua")));
//        script.setResultType(Boolean.class);
//
//        return reactiveRedisTemplate.execute(script, List.of(activeKey, userId))
//                .doOnNext(success -> {
//                    if (success) {
//                        log.info("슬롯 해제 성공 [Festival: {}, User: {}]", festivalId, userId);
//                    } else {
//                        log.warn("슬롯 해제 실패 (슬롯이 이미 0) [Festival: {}, User: {}]", festivalId, userId);
//                    }
//                }).then();
//    }
}
