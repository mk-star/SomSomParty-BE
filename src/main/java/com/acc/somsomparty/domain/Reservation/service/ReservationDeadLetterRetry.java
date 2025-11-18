package com.acc.somsomparty.domain.Reservation.service;

import com.acc.somsomparty.domain.Reservation.config.RabbitMQConfig;
import com.acc.somsomparty.domain.Reservation.listener.ReservationEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * 데드레터로 들어온 메시지를 Requeue 한다.
 */
@Component
public class ReservationDeadLetterRetry {

    @Value("${slack.webhook.url}")
    private String slackWebhookUrl;

    private final WebClient webClient;

    public ReservationDeadLetterRetry(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(slackWebhookUrl).build();
    }

    @RabbitListener(queues = RabbitMQConfig.DLQ)
    public void processDlqMessage(ReservationEvent event) {
        System.out.println("[DLQ Received]: " + event);

        String slackMessage = createSlackMessage(event);

        sendSlackNotification(slackMessage)
                .doOnSuccess(response ->
                        System.out.println("Slack notification sent successfully."))
                .doOnError(error ->
                        System.err.println("Error sending Slack notification: " + error.getMessage()))
                .subscribe();
    }

    private String createSlackMessage(ReservationEvent event) {
        return String.format(
                "🚨 *DLQ 알림 - 예약 처리 실패* 🚨\n" +
                        "*발생 시간:* %s\n" +
                        "*원본 이벤트:* %s\n" +
                        "*처리 큐:* %s\n" +
                        "*담당자 확인 필요!*",
                LocalDateTime.now(),
                event.toString(),
                RabbitMQConfig.RESERVATION_COMPLETED_QUEUE // 원본 큐 이름
        );
    }

    private Mono<String> sendSlackNotification(String text) {
        String payload = String.format("{\"text\": \"%s\"}", text.replace("\n", "\\n"));

        return webClient.post()
                .uri(slackWebhookUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(e -> {
                    // HTTP 통신 자체의 실패를 처리 (예: 네트워크 오류, DNS 문제 등)
                    System.err.println("WebClient error during Slack call: " + e.getMessage());
                    return Mono.just("Slack_Error_Handled");
                });
    }

}