package com.acc.somsomparty.domain.Reservation.kafka.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {
    @Value("${spring.kafka.bootstrap_servers}")
    private String BOOTSTRAP_SERVERS;
//    private final KafkaListenerEndpointRegistry registry;
//    private final ListenerContainerPauseService pauser;
//    private final DelayTimeCalculator delayTimeCalculator;

//    public KafkaConfig(
//            KafkaListenerEndpointRegistry registry,
//            ListenerContainerPauseService pauser,
//            DelayTimeCalculator delayTimeCalculator
//    ) {
//        this.registry = registry;
//        this.pauser = pauser;
//        this.delayTimeCalculator = delayTimeCalculator;
//    }

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> props = new HashMap<>();

        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS); // Producer가 처음으로 연결할 Kafka 브로커의 위치

        // Kafka는 네트워크를 통해 데이터를 전송하기 때문에, 객체를 byte array로 변환하는 직렬화 과정이 필요
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        props.put(ProducerConfig.ACKS_CONFIG, "all"); // 모든 복제본에 데이터 전송 완료 시 성공 응답
        props.put(ProducerConfig.RETRIES_CONFIG, 3); // 메시지 전송 실패 시 재시도 횟수
        props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 500); // 재시도 간격 (500ms)
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true); // 중복 방지 (Idempotent Producer)


        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 50); // max.poll.records 설정
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // manual ackmode
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // interceptor
//        props.put(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG, KafkaThrottlingInterceptor.class.getName());
//        props.put(KafkaThrottlingInterceptor.KAFKA_LISTENER_ENDPOINT_REGISTRY_CONFIG_KEY, this.registry);
//        props.put(KafkaThrottlingInterceptor.PAUSE_SERVICE_CONFIG_KEY, this.pauser);
//        props.put(KafkaThrottlingInterceptor.PAUSE_TIME_CALCULATOR_CONFIG_KEY, this.delayTimeCalculator);

        return new DefaultKafkaConsumerFactory<>(props);
    }

    // 메시지를 수신하는 KafkaListenerContainerFactory 빈 정의
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }

    // 원래는 축제 생성 시 자동 생성
//    @Bean
//    public NewTopic newTopic() {
//        return TopicBuilder.name("queue-wait-1")
//                .partitions(3)
//                .replicas(1)
//                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(1000 * 60 * 60)) // 1시간
//                .build();
//    }

    @Bean
    public DefaultErrorHandler dlqErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        DeadLetterPublishingRecoverer dlqRecover = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, e) -> {
//                    EventLogger.logMessageConsumeError((ConsumerRecord<String, String>) record, getOriginalException(e));
                    return new TopicPartition(record.topic() + ".dlq", record.partition());
                });
        return new DefaultErrorHandler(dlqRecover, new FixedBackOff(1000L, 3L));
    }

    private static Exception getOriginalException(Exception e) {
        Throwable cause = e.getCause();
        return (cause instanceof Exception) ? (Exception) cause : new Exception(cause);
    }
}
