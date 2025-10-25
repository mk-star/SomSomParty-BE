package com.acc.somsomparty.domain.Reservation.kafka;

import com.acc.somsomparty.domain.Reservation.kafka.event.ReservationEvent;
import com.acc.somsomparty.domain.Reservation.service.ReservationCommandService;
import com.acc.somsomparty.global.util.EventSerializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class QueueConsumer {
    private final ReservationCommandService reservationCommandService;
    private final KafkaListenerEndpointRegistry registry;

    @KafkaListener(
            id = "reservation-completed-listener",
            topics = "reservation-completed",
            groupId = "reservation-group",
            concurrency = "3")
    public void handleReservationCompletedEvent(ConsumerRecord<String, String> record, Acknowledgment ack) {
        ReservationEvent event = EventSerializer.deserialize(record.value(), ReservationEvent.class);

//        final MessageListenerContainer container = getContainer(record);
//        log.info("Processing Message: {}, partition: {}, container: {}", event, topicPartition(record), container.getListenerId());
        reservationCommandService.makeReservation(event);

        // acknowledge
        ack.acknowledge();
//        log.info("Ack Message: {}, partition: {}, container: {}", event, topicPartition(record), container.getListenerId());
    }

    private MessageListenerContainer getContainer(ConsumerRecord<String, String> record) {
        final var concurrentContainer = (ConcurrentMessageListenerContainer<String, String>) registry.getListenerContainer("reservation-completed-listener");
        return concurrentContainer.getContainerFor(record.topic(), record.partition());
    }

    private String topicPartition(ConsumerRecord<String, String> record) {
        return record.topic() + "-" + record.partition();
    }
}
