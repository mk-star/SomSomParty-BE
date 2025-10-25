package com.acc.somsomparty.domain.Reservation.kafka;

import com.acc.somsomparty.domain.Reservation.kafka.event.ReservationEvent;
import com.acc.somsomparty.domain.Reservation.kafka.topics.EventTopic;
import com.acc.somsomparty.global.util.EventSerializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class QueueProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;

    public void publishEvent(ReservationEvent event) {
        String message = EventSerializer.serialize(event);
        kafkaTemplate.send(EventTopic.RESERVATION_COMPLETED.getTopic(), message);
    }
}

