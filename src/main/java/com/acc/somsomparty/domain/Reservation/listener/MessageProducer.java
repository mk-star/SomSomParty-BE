package com.acc.somsomparty.domain.Reservation.listener;

import com.acc.somsomparty.domain.Reservation.config.RabbitMQConfig;
import com.acc.somsomparty.domain.Reservation.repository.ReservationRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;


@Component
public class MessageProducer {
    private final RabbitTemplate rabbitTemplate;

    public MessageProducer(RabbitTemplate rabbitTemplate, RedisTemplate<String, String> redisTemplate, ReservationRepository reservationRepository) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional
    public void sendMessage(ReservationEvent event) {
        // 메시지를 rabbitmq에 전송
        // CorrelationData은 퍼블리셔 컨펌에서 사용되는 객체로 메시지 전송 상태를 추적하기 위해 사용됨
        // 성공, 실패에 따라 추가 로직을 작성하기 위해

        String eventId = UUID.randomUUID().toString();
        MyCorrelationData correlationData = new MyCorrelationData(eventId, event);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.RESERVATION_COMPLETED_EXCHANGE,
                RabbitMQConfig.ROUTING_KEY,
                event,
                correlationData
        );

    }
}