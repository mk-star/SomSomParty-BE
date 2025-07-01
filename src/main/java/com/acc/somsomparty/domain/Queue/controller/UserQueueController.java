package com.acc.somsomparty.domain.Queue.controller;

import com.acc.somsomparty.domain.Queue.dto.AllowedUserResponse;
import com.acc.somsomparty.domain.Queue.dto.FestivalWaitingRoomResponse;
import com.acc.somsomparty.domain.Queue.dto.RankNumberResponse;
import com.acc.somsomparty.domain.Queue.service.UserQueueService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/queues")
@RequiredArgsConstructor
public class UserQueueController {
    private final UserQueueService userQueueService;

    @Operation(summary = "대기열 등록", description = "유저를 대기열에 등록합니다. 이미 등록된 유저라면, 현재 대기열에서의 순위를 반환합니다.(email 임시 설정)")
    @GetMapping("/{queue}/waiting-room/users/{email}")
    Mono<FestivalWaitingRoomResponse> waitingRoomPage(@PathVariable String queue, @PathVariable String email) {
        return userQueueService.isAllowed(queue, email) // true(이미 대기 완료), false(아직 대기 안함)
                // allowed가 true라면, filter를 통과하여 다음 단계인 flatMap이 실행
                // 반면, allowed가 false라면, filter에서 걸러져서 빈 Mono가 반환.
                .filter(allowed -> allowed)
                // filter에서 true 면 flatMap이 진행
                .flatMap(allowed -> Mono.just(new FestivalWaitingRoomResponse(true, null)))
                // filter에서 빈 Mono가 반환되면서 switchIfEmpty가 진행됨. 유저를 대기열에 넣는 메서드(registerWaitQueue)가 실행됨.
                .switchIfEmpty(
                        userQueueService.registerWaitQueue(queue, email)
                                .onErrorResume(ex -> userQueueService.getRank(queue, email)) // 유저가 이미 queue에 등록 되었다면 rank를 반환
                                .map(rank -> new FestivalWaitingRoomResponse(false, rank))
                );
    }

    @Operation(summary = "대기열에서 유저 순위 반환", description = "유저가 대기열에서 몇 번째 순위인지를 알려줍니다.")
    @GetMapping("/{queue}/users/{email}/rank")
    public Mono<RankNumberResponse> getRankUser(@PathVariable String queue, @PathVariable String email) {
        // -1이 return 되면 예약 페이지로 이동
        return userQueueService.getRank(queue, email)
                .map(RankNumberResponse::new);
    }

    @Operation(summary = "예약 페이지 진입 가능 여부 반환", description = "예약 페이지 진입이 가능한지 다시 한번 더 확인합니다.")
    @GetMapping("/{queue}/users/{email}/allowed")
    public Mono<AllowedUserResponse> isAllowedUser(@PathVariable String queue, @PathVariable String email) {
        return userQueueService.isExistInWaitOrProceed(queue, "proceed", email)
                .map(AllowedUserResponse::new);
    }

    @Operation(summary = "대기열/대기완료 열 삭제", description = "사용자를 대기열 / 대기완료 열에서 삭제합니다. ")
    @DeleteMapping("/{queue}/users/{email}/leave")
    Mono<Void> leaveWaitQueue(
            @PathVariable String queue,
            @PathVariable String email,
            @RequestParam(defaultValue = "wait") String queueType) {
        return userQueueService.removeUserFromQueue(queue, queueType, email);
    }
}