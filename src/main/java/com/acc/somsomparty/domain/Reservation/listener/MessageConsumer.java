package com.acc.somsomparty.domain.Reservation.listener;

import com.acc.somsomparty.domain.Reservation.config.RabbitMQConfig;
import com.acc.somsomparty.domain.Reservation.exception.SoldOutException;
import com.acc.somsomparty.domain.Reservation.service.ReservationCommandService;
import com.rabbitmq.client.Channel;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class MessageConsumer {

    private final RetryTemplate retryTemplate;
    private final ReservationCommandService reservationCommandService;

    public MessageConsumer(RetryTemplate retryTemplate, ReservationCommandService reservationCommandService) {
        this.retryTemplate = retryTemplate;
        this.reservationCommandService = reservationCommandService;
    }

    @RabbitListener(queues = RabbitMQConfig.RESERVATION_COMPLETED_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    public void receiveMessage(ReservationEvent event,
                               @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                               Channel channel) {

        try {
            retryTemplate.execute(context -> {
                try {
                    System.out.println("Received Message: " + event + " count: " + context.getRetryCount());

                    reservationCommandService.makeReservation(event);

                    channel.basicAck(deliveryTag, false);

                } catch (SoldOutException e) {
                    channel.basicNack(deliveryTag, false, false);
                } catch (Exception e) {
                    System.out.println("[Consumer Error] " + e.getMessage());
                    // 재시도 횟수가 3번을 넘으면 DLQ로 이동
                    if (context.getRetryCount() >= 2) {
                        channel.basicNack(deliveryTag, false, false);
                    }  else {
                        throw e;
                    }
                }
                return null;
            });
        } catch (IOException ex) {
            System.out.println("[Consumer send nack] " + ex.getMessage());
            //throw new RuntimeException(e);
        }
    }
}