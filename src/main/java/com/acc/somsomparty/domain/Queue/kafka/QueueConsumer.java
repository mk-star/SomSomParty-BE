package com.acc.somsomparty.domain.Queue.kafka;

import com.acc.somsomparty.domain.Queue.dto.QueueMessage;
import com.acc.somsomparty.domain.Queue.service.QueueService;
import com.acc.somsomparty.domain.Queue.service.SlotService;
import com.acc.somsomparty.domain.Reservation.aop.DistributedLock;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Service
@Slf4j
@RequiredArgsConstructor
public class QueueConsumer {
    private final ObjectMapper objectMapper;
    private final SlotService slotService;
    private final QueueService queueService;

    @KafkaListener(topicPattern = "queue-wait-.*", groupId = "waiting-group")
    public void consume(ConsumerRecord<String, String> record) {
        String topic = record.topic();
        String festivalId = topic.replace("queue-wait-", "");
        String message = record.value();

        try {
            QueueMessage queueMessage = parseMessage(message);
            log.info("Kafka 메시지 수신 [Festival: {}, User: {}, CurrentTime: {}]",
                    festivalId, queueMessage.userId(), queueMessage.currentTime());

            processQueue(festivalId, queueMessage.userId()).subscribe();
        } catch (Exception e) {
            log.error("Kafka 메시지 처리 실패 [Topic: {}, Message: {}, Error: {}]", topic, message, e.getMessage());
        }
    }

    private QueueMessage parseMessage(String message) {
        try {
            return objectMapper.readValue(message, QueueMessage.class);
        } catch (Exception e) {
            log.error("Kafka 메시지 역직렬화 실패 [Message: {}]", message, e);
            return null;
        }
    }

    public Mono<Void> processQueue(String festivalId, String userId) {
        return slotService.acquireSlotAndPopUser(festivalId)
                .flatMap(result -> {
                    if ("EMPTY".equals(result)) {
                        log.info("대기열 비어 있음, 처리 종료 [Festival: {}]", festivalId);
                        return Mono.empty();
                    }
                    if ("NOSLOT".equals(result)) { // 슬롯 없으면 예외 발생 → retryWhen에서 재시도 처리
                        log.info("슬롯이 없음 [Festival: {}, User: {}]", festivalId, userId);
                        return Mono.error(new RuntimeException("No slot"));
                    }

                    log.info("사용자 입장 처리 [Festival: {}, User: {}]", festivalId, result);

                    // rank 전송을 비동기 처리
                    return queueService.sendRank(festivalId, result);
                })
                .retryWhen(Retry.backoff(5, Duration.ofMillis(100))
                        .filter(ex -> "No slot".equals(ex.getMessage())))
                .onErrorResume(ex -> {
                    log.warn("슬롯 재시도 초과, 유저 스킵 [Festival: {}, Error: {}]", festivalId, ex.getMessage());
                    return Mono.empty();
                })
                .then(queueService.sendRanks(festivalId));
    }
}
