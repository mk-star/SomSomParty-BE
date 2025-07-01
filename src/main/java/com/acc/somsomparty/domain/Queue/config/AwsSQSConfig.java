package com.acc.somsomparty.domain.Queue.config;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;

import static software.amazon.awssdk.regions.Region.AP_NORTHEAST_2;

import io.awspring.cloud.sqs.config.SqsBootstrapConfiguration;
import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementOrdering;
import io.awspring.cloud.sqs.listener.acknowledgement.handler.AcknowledgementMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;


import java.time.Duration;

@Import(SqsBootstrapConfiguration.class)
@Slf4j
@Configuration
public class AwsSQSConfig {

    @Value("${cloud.aws.credentials.accessKey}")
    private String AWS_ACCESS_KEY;

    @Value("${cloud.aws.credentials.secretKey}")
    private String AWS_SECRET_KEY;

    @Value("${cloud.aws.region.static}")
    private String AWS_REGION;

    // 클라이언트 설정: region과 자격증명
    @Bean
    public SqsAsyncClient sqsAsyncClient() {
        return SqsAsyncClient.builder()
                .credentialsProvider(() -> new AwsCredentials() {
                    @Override
                    public String accessKeyId() {
                        return AWS_ACCESS_KEY;
                    }

                    @Override
                    public String secretAccessKey() {
                        return AWS_SECRET_KEY;
                    }
                })
                .region(Region.of(AWS_REGION))
                .build();
    }

    // 메시지 발송을 위한 SQS 템플릿 설정 (Sender 쪽)
    @Bean
    public SqsTemplate sqsTemplate() {
        return SqsTemplate.newTemplate(sqsAsyncClient());
    }

    @Bean
    SqsMessageListenerContainerFactory<Object> defaultSqsListenerContainerFactory(SqsAsyncClient sqsAsyncClient) {
        return SqsMessageListenerContainerFactory
                .builder()
                .configure(options -> options
                        .acknowledgementMode(AcknowledgementMode.MANUAL) // 명시적으로 acknowledgement를 해야 메시지가 삭제됨
                        .acknowledgementOrdering(AcknowledgementOrdering.ORDERED) // 순서대로 처리
                )
                .sqsAsyncClient(sqsAsyncClient)
                .build();
    }
}