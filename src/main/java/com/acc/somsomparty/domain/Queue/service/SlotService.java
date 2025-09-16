package com.acc.somsomparty.domain.Queue.service;

import com.acc.somsomparty.domain.Queue.dto.QueueStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlotService {
    private final String SLOT_ACTIVE_KEY = "slots:active:{%s}";
    private final String SLOT_MAX_KEY = "slots:max:{%s}";
    private final String QUEUE_WAIT_KEY = "queue:wait:{%s}";
    private final RedisTemplate<String, String> redisTemplate;

    // 슬롯 초기화
    public void initializeSlot(String festivalId, int maxSlot) {
        String activeKey = SLOT_ACTIVE_KEY.formatted(festivalId);
        String maxKey = SLOT_MAX_KEY.formatted(festivalId);

        redisTemplate.opsForValue().set(activeKey, "0"); // 활성 슬롯 초기화
        redisTemplate.opsForValue().set(maxKey, String.valueOf(maxSlot)); // 최대 슬롯 설정
        log.info("슬롯 초기화 완료 [Festival: {}, MaxSlot: {}]", festivalId, maxSlot);
    }

    // 저장 가능한 슬롯의 개수가 몇 개인지 반환
    public QueueStatus getQueueStatus(String festivalId) {
        String activeKey = SLOT_ACTIVE_KEY.formatted(festivalId);
        String maxKey = SLOT_MAX_KEY.formatted(festivalId);
        String queueKey = QUEUE_WAIT_KEY.formatted(festivalId);

        int active = Optional.ofNullable(redisTemplate.opsForValue().get(activeKey))
                .map(Integer::parseInt)
                .orElse(0);

        int max = Optional.ofNullable(redisTemplate.opsForValue().get(maxKey))
                .map(Integer::parseInt)
                .orElse(0);

        Long queueSize = redisTemplate.opsForZSet().size(queueKey);
        if (queueSize == null) queueSize = 0L;

        long availableSlots = Math.min(queueSize, max - active);

        return new QueueStatus(availableSlots, queueSize);
    }

    // 최대 슬롯 수 확인하고 점유 가능하면 active 슬롯 증가하고 pop한 사용자 ID 반환
    public String acquireSlotAndPopUser(String festivalId) {
        String queueKey = QUEUE_WAIT_KEY.formatted(festivalId);
        String activeKey = SLOT_ACTIVE_KEY.formatted(festivalId);
        String maxKey = SLOT_MAX_KEY.formatted(festivalId);

        DefaultRedisScript<String> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/acquire_and_pop.lua")));
        script.setResultType(String.class);

        return redisTemplate.execute(script, List.of(queueKey, activeKey, maxKey));
    }

    // 유저가 대기열에서 나가면 끝나면 슬롯을 해제함
    public void releaseSlot(String festivalId, String userId) {
        String activeKey = SLOT_ACTIVE_KEY.formatted(festivalId);

        DefaultRedisScript<Boolean> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/release_slot.lua")));
        script.setResultType(Boolean.class);

        Boolean success = redisTemplate.execute(script, List.of(activeKey));
        if (Boolean.TRUE.equals(success)) {
            log.info("슬롯 해제 성공 [Festival: {}, User: {}]", festivalId, userId);
        } else {
            log.warn("슬롯 해제 실패 (슬롯이 이미 0) [Festival: {}, User: {}]", festivalId, userId);
        }
    }
}
