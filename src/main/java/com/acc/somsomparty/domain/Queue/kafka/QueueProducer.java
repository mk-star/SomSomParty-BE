package com.acc.somsomparty.domain.Queue.kafka;

import com.acc.somsomparty.domain.Queue.dto.QueueMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class QueueProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private static final String topic = "queue-wait-%s";

    public void enqueueUser(String festivalId, String userId) {
        QueueMessage message = createQueueMessage(userId);
        sendQueueMessage(festivalId, message);
    }

    private QueueMessage createQueueMessage(String userId) {
        return QueueMessage.builder()
                .userId(userId)
                .currentTime(Instant.now().toEpochMilli())
                .build();
    }

    private void sendQueueMessage(String festivalId, QueueMessage message) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);

            kafkaTemplate.send(topic.formatted(festivalId), jsonMessage)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            RecordMetadata m = result.getRecordMetadata();
                            log.info("전송 성공 [User: {}, Time: {}, Partition: {}, Offset: {}]",
                                    message.userId(), message.currentTime(), m.partition(), m.offset());
                        } else {
                            log.error("전송 실패 [User: {}, Time: {}, Error: {}]",
                                    message.userId(), message.currentTime(), ex.getMessage());
                        }
                    });
        } catch (JsonProcessingException e) {
            log.error("메시지 변환 실패 [User: {}, Error: {}]", message.userId(), e.getMessage());
        }
    }
}
