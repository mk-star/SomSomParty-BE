package com.acc.somsomparty.domain.Queue.kafka;

import com.acc.somsomparty.domain.Queue.dto.QueueMessage;
import com.acc.somsomparty.domain.Queue.service.QueueMonitorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class QueueConsumer {
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final ObjectMapper objectMapper;
    private final QueueMonitorService queueMonitorService;
    private final String QUEUE_WAIT_KEY = "queue:wait:%s";

    @KafkaListener(topicPattern = "queue-wait-.*", groupId = "waiting-group")
    public void consume(ConsumerRecord<String, String> record) {
        String topic = record.topic();
        String festivalId = topic.replace("queue-wait-", "");
        String message = record.value();

        try {
            QueueMessage queueMessage = parseMessage(message);
            log.info("Kafka 메시지 수신 [Festival: {}, User: {}, CurrentTime: {}]",
                    festivalId, queueMessage.userId(), queueMessage.currentTime());

            addUserToQueue(festivalId, queueMessage.userId(), queueMessage.currentTime())
                    .then(queueMonitorService.sendQueueRank(festivalId, queueMessage.userId()))
                    .subscribe();
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

    private Mono<Boolean> addUserToQueue(String festivalId, String userId, double score) {
        return reactiveRedisTemplate.opsForZSet()
                .add(QUEUE_WAIT_KEY.formatted(festivalId), userId, score)
                .doOnSuccess(added -> {
                    if (Boolean.TRUE.equals(added)) {
                        log.info("Redis ZSet에 추가됨 [Festival: {}, User: {}, Score: {}]", festivalId, userId, score);
                    } else {
                        log.warn("이미 존재하는 사용자 [Festival: {}, User: {}]", festivalId, userId);
                    }
                })
                .doOnError(ex -> log.error("Redis ZSet 추가 실패 [Festival: {}, User: {}, Error: {}]", festivalId, userId, ex.getMessage()));
    }
}
