package com.acc.somsomparty.domain.Queue.controller;

import com.acc.somsomparty.domain.Queue.service.QueueService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/queues")
@RequiredArgsConstructor
public class QueueRestController {
    private final QueueService queueService;

    @Operation(summary = "대기열 등록", description = "유저를 대기열에 등록합니다. 이미 등록된 유저라면, 현재 대기열에서의 순위를 반환합니다.(email 임시 설정)")
    @GetMapping("/{festivalId}/waiting-room/users/{userId}")
    Mono<Void> waitingRoomPage(@PathVariable String festivalId, @PathVariable String userId) {
          return queueService.registerWaitQueue(festivalId, userId);
    }

    @Operation(summary = "대기열 삭제", description = "사용자를 대기열에서 삭제합니다. ")
    @DeleteMapping("/{festivalId}/users/{userId}/leave")
    Mono<Void> leaveWaitQueue(@PathVariable String festivalId, @PathVariable String userId) {
        return queueService.removeUserFromQueue(festivalId, userId);
    }
}
