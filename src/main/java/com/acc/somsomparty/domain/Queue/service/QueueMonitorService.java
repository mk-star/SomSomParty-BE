package com.acc.somsomparty.domain.Queue.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueueMonitorService {
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final QueueService queueService; // 대기열 관련 서비스
    private final SlotService slotService; // 슬롯 관련 서비스
    private final ConcurrentHashMap<String, Boolean> monitoringStatus = new ConcurrentHashMap<>();

    public Mono<Void> startMonitoring(String festivalId) {
        if (monitoringStatus.putIfAbsent(festivalId, true) != null) {
            log.info("축제에 대한 모니터링이 이미 실행 중 [Festival: {}]", festivalId);
            return Mono.empty();
        }

        log.info("축제에 대한 모니터링 시작 [Festival: {}]", festivalId);

        return Flux.interval(Duration.ofSeconds(5))
                .takeWhile(tick -> monitoringStatus.containsKey(festivalId))
                .flatMap(tick -> queueService.isQueueEmpty(festivalId)
                        .flatMap(isEmpty -> {
                            if (isEmpty) {
                                log.info("대기열 비어 있음 [Festival: {}]", festivalId);
                                return Mono.just(false); // 대기열 비어있으면 종료
                            }
                            // 대기열에 사람이 있으면 슬롯 여부 확인하고 입장 처리 시도
                            return slotService.hasAvailableSlot(festivalId)
                                    .flatMap(hasSlot -> {
                                        if (!hasSlot) {
                                            log.info("입장 슬롯 없음 [Festival: {}]", festivalId);
                                            return Mono.just(true); // 슬롯 없지만 대기열 있으니 계속 모니터링
                                        }
                                        return enterNextUsersFromQueue(festivalId).thenReturn(true);
                                    });
                        })
                )
                .takeUntil(continueMonitoring -> !continueMonitoring)
                .doFinally(signal -> {
                    log.info("축제 모니터링 종료 [Festival: {}]", festivalId);
                    monitoringStatus.remove(festivalId);
                })
                .then();
    }

    private Mono<Void> enterNextUsersFromQueue(String festivalId) {
        return slotService.getAvailableSlots(festivalId)
                .flatMapMany(availableSlots -> {
                    if (availableSlots <= 0) {
                        log.warn("점유 가능한 슬롯이 없음 [Festival: {}]", festivalId);
                        return Flux.empty();
                    }
                    return Flux.range(1, Math.toIntExact(availableSlots));
                })
                .flatMap(i -> slotService.acquireSlotAndPopUser(festivalId)  // 병렬 처리
                        .flatMap(userId -> {
                            if (userId == null || userId.isEmpty()) {
                                log.warn("대기열에서 사용자 ID를 가져올 수 없음 [Festival: {}]", festivalId);
                                return Mono.empty();
                            }
                            log.info("사용자 입장 처리 [Festival: {}, User: {}]", userId, festivalId);
                            // 실제 입장 처리 로직 추가
                            return Mono.empty();
                        })
                )
                .then();
    }

    public Mono<Void> sendQueueRank(String festivalId, String userId) {
        return Flux.interval(Duration.ofSeconds(5))
                .flatMap(tick -> queueService.getRank(festivalId, userId))
                .doOnNext(rank -> {
                    String message = buildMessage(festivalId, userId, rank);
                    messagingTemplate.convertAndSend("/topic/queue.status." + festivalId + "." + userId, message);
                })
                .takeWhile(rank -> rank != -1L)
                .then();
    }

    public Mono<Void> leaveQueue(String festivalId) {
        return slotService.releaseSlot(festivalId);
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
}
