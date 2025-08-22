package com.acc.somsomparty.domain.Queue.controller;

import com.acc.somsomparty.domain.Queue.service.QueueMonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class QueueWebSocketController {
    private final QueueMonitorService queueMonitorService;

    // 대기열을 모니터링하면서 유저를 pop
    @MessageMapping("/queue.{festivalId}")
    public void startMonitoring(@DestinationVariable String festivalId) {
        queueMonitorService.startMonitoring(festivalId).subscribe();
    }

    // 대기열에 있는 모든 유저 처리 시
    @MessageMapping("/queue.{festivalId}.leave")
    public void leaveQueue(@DestinationVariable String festivalId) {
        queueMonitorService.leaveQueue(festivalId).subscribe();
    }
}
