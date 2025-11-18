package com.acc.somsomparty.domain.Reservation.config;

import com.acc.somsomparty.domain.Reservation.listener.MyCorrelationData;
import com.acc.somsomparty.domain.Reservation.listener.ReservationEvent;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String RESERVATION_COMPLETED_QUEUE = "reservationQueue";
    public static final String RESERVATION_COMPLETED_EXCHANGE = "reservationExchange";
    public static final String ROUTING_KEY = "reservationRoutingKey";

    public static final String DLQ = "deadLetterQueue";
    public static final String RESERVATION_COMPLETED_DLX = "deadLetterExchange";
    public static final String DEAD_LETTER_ROUTING_KEY = "dead.letter";

    // Queue 설정
    @Bean
    public Queue reservationQueue() {
        return QueueBuilder.durable(RESERVATION_COMPLETED_QUEUE)
                .withArgument("x-dead-letter-exchange", RESERVATION_COMPLETED_DLX) // Dead Letter Exchange
                .withArgument("x-dead-letter-routing-key", DEAD_LETTER_ROUTING_KEY) // Dead Letter Routing Key
                .build();
    }

    // Dead Letter Queue 설정
    @Bean
    public Queue deadLetterQueue() {
        return new Queue(DLQ);
    }

    // Exchange 설정
    @Bean
    public DirectExchange reservationExchange() {
        return new DirectExchange(RESERVATION_COMPLETED_EXCHANGE);
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(RESERVATION_COMPLETED_DLX);
    }

    // Binding 설정
    @Bean
    public Binding reservationBinding() {
        return BindingBuilder.bind(reservationQueue()).to(reservationExchange()).with(ROUTING_KEY);
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with(DEAD_LETTER_ROUTING_KEY);
    }

    // 메시지 변환기 설정
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // RabbitTemplate 설정, ReturnsCallback 활성화 등록, ConfirmCallback 설정
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter()); // JSON 변환기
        rabbitTemplate.setMandatory(true);  // ReturnCallback 활성화
       //rabbitTemplate.setRetryTemplate(retryTemplate());

        // ConfirmCallBack 설정
        // Producer -> [publish] -> Exchange
        // 메시지가 exchange에 잘 도착했느냐를 확인하는 callback
        // 컨펌이 떨어져야 실제 트랜잭션 처리를 함
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                //  메시지가 exchange에 도달하였다.
                System.out.println("#### [Message confirmed]: " +
                        (correlationData != null ? correlationData.getId() : "null"));
                System.out.println(correlationData);
            } else {
                // 메시지가 exchange에 도달하지 못했다
                System.out.println("#### [Message not confirmed]: " +
                        (correlationData != null ? correlationData.getId() : "null") + ", Reason: " + cause);
                
                // 실패 메시지에 대해 DLQ에 넣어 수동 재처리
                if (correlationData instanceof MyCorrelationData myData) {
                    ReservationEvent failedEvent = myData.getEvent();
                    rabbitTemplate.convertAndSend(RabbitMQConfig.RESERVATION_COMPLETED_DLX,
                            RabbitMQConfig.DEAD_LETTER_ROUTING_KEY, failedEvent);
                }
            }
        });

        // ReturnCallback 설정
        // Exchange -> [route] -> Queue
        // exchange에서 queue로 라우팅하지 못했을 경우에 동작
        // setMandatory(true)로 해서 리턴 콜백이 활성화가 됨
        rabbitTemplate.setReturnsCallback(returned -> {
            System.out.println("Return Message: " + returned.getMessage().getBody());
            System.out.println("Exchange : " + returned.getExchange());
            System.out.println("RoutingKey : " + returned.getRoutingKey());

            Object eventObject = messageConverter().fromMessage(returned.getMessage());
            if (eventObject instanceof ReservationEvent event) {
                rabbitTemplate.convertAndSend(RabbitMQConfig.RESERVATION_COMPLETED_DLX,
                        RabbitMQConfig.DEAD_LETTER_ROUTING_KEY, event);
            }
        });
        return rabbitTemplate;
    }

    // RabbitListener 설정, 수동 Ack 모드 설정
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL); // 수동 Ack 모드
        return factory;
    }

}