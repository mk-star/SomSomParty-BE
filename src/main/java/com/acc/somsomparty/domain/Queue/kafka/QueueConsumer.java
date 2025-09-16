package com.acc.somsomparty.domain.Queue.kafka;

import com.acc.somsomparty.domain.Queue.dto.QueueMessage;
import com.acc.somsomparty.domain.Queue.dto.QueueStatus;
import com.acc.somsomparty.domain.Queue.service.QueueService;
import com.acc.somsomparty.domain.Queue.service.SlotService;
import com.acc.somsomparty.global.exception.CustomException;
import com.acc.somsomparty.global.exception.error.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class QueueConsumer {
    private final ObjectMapper objectMapper;
    private final SlotService slotService;
    private final QueueService queueService;

    @KafkaListener(topicPattern = "queue-wait-.*", concurrency = "4")
    public void consume(ConsumerRecord<String, String> record) {
        String topic = record.topic();
        String festivalId = topic.replace("queue-wait-", "");
        String message = record.value();

        try {
            QueueMessage queueMessage = parseMessage(message);
            log.info("Kafka 메시지 수신 [Festival: {}, User: {}, CurrentTime: {}]",
                    festivalId, queueMessage.userId(), queueMessage.currentTime());

            processQueue(festivalId);
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

    private void processQueue(String festivalId) {
        int maxRetry = 5;
        int retryDelay = 100; // ms

        for (int attempt = 1; attempt <= maxRetry; attempt++) {
            try {
                QueueStatus status = slotService.getQueueStatus(festivalId);

                if (status.getQueueSize() == 0) {
                    log.info("대기열 비어 있음, 처리 종료 [Festival: {}]", festivalId);
                    return;
                }

                long availableSlots = status.getAvailableSlots();
                if (availableSlots == 0) {
                    log.info("슬롯 없음, 재시도 [{} / {}] [Festival: {}]", attempt, maxRetry, festivalId);
                    throw new CustomException(ErrorCode.NO_SLOT);
                }

                for (int i = 0; i < availableSlots; i++) {
                    String userId = slotService.acquireSlotAndPopUser(festivalId);
                    if (userId == null) {
                        log.info("대기열 비어 있음, 처리 종료 [Festival: {}]", festivalId);
                        break;
                    }

                    log.info("사용자 입장 처리 [Festival: {}, User: {}]", festivalId, userId);

                    try {
                        queueService.sendRank(festivalId, userId);
                    } catch (Exception e) {
                        log.error("Rank 전송 실패 [Festival: {}, User: {}, Error: {}]", festivalId, userId, e.getMessage());
                    }
                    slotService.releaseSlot(festivalId, userId);
                }

                queueService.sendRanks(festivalId);
                break; // 성공하면 재시도 루프 종료
            } catch (Exception e) {
                log.error("예약 처리 실패. 재시도 시도: {}/{}", attempt, maxRetry, e);

                if (attempt == maxRetry) {
                    log.warn("재시도 최대 횟수 초과, 유저 스킵 [Festival: {}]", festivalId);
                    break;
                }

                try {
                    Thread.sleep(retryDelay); // 딜레이 적용
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("재시도 중 인터럽트 발생.", ie);
                }

                retryDelay *= 2;
            }
        }
    }
}
