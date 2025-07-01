package com.acc.somsomparty.domain.Queue.service;

import com.acc.somsomparty.domain.Queue.config.SqsSender;
import com.acc.somsomparty.global.exception.CustomException;
import com.acc.somsomparty.global.exception.error.ErrorCode;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement;
import org.redisson.api.RFuture;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.util.function.Tuples;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserQueueService {
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final RedissonClient redissonClient;
    private final SqsSender sqsSender;

    // 사용자 대기 queue의 key
    private final String USER_QUEUE_WAIT_KEY = "users:queue:%s:wait";
    // 사용자 대기 queue를 scan 하기 위한 key
    private final String USER_QUEUE_WAIT_KEY_FOR_SCAN = "users:queue:*:wait";
    // 사용자 대기 완료 queue의 key
    private final String USER_QUEUE_PROCEED_KEY = "users:queue:%s:proceed";

    // 유저 대기열 등록
    // redis의 sorted set을 대기열로 사용 ( key : user PK , value : unix timestamp)
    // 현재 본인의 순위를 return 함
    public Mono<Long> registerWaitQueue(String queue, String email) {
        // 대기열에 사용자 존재 여부
        Mono<Boolean> existsInWaitQueue = isExistInWaitOrProceed(queue, "wait", email);

        // 대기 완료 열 사용자 존재 여부
        Mono<Boolean> existsInProceedQueue = isExistInWaitOrProceed(queue, "proceed", email);

        long unixTimestamp = Instant.now().toEpochMilli(); // 현재 시간
        return Mono.zip(existsInWaitQueue, existsInProceedQueue)
                .flatMap(tuple -> {
                    boolean inWait = tuple.getT1();
                    boolean inProceed = tuple.getT2();

                    if (inWait || inProceed) {
                        return Mono.error(new CustomException(ErrorCode.QUEUE_ALREADY_REGISTERED_USER));
                    }

                    return reactiveRedisTemplate.opsForZSet()
                            .add(USER_QUEUE_WAIT_KEY.formatted(queue), email, unixTimestamp)
                            .filter(i -> i)
                            .switchIfEmpty(Mono.error(new CustomException(ErrorCode.QUEUE_ALREADY_REGISTERED_USER)))
                            .flatMap(i -> {
                                // 입장 처리 타이밍을 컨트롤하기 위한 단순 트리거 역할
                                String messageContent = email + "가 " + queue + " 대기열로 입장함";
                                sqsSender.send(messageContent);
                                return reactiveRedisTemplate.opsForZSet().rank(USER_QUEUE_WAIT_KEY.formatted(queue), email);
                            })
                            .map(i -> i >= 0 ? i + 1 : i)
                            .doOnSuccess(result -> log.info("사용자 {}가 {}번째로 대기열 등록 성공", email, result));
                });
    }

    // 대기열 or 대기 완료 열에서 사용자 존재 여부 확인
    public Mono<Boolean> isExistInWaitOrProceed(String queue, String queueType, String email) {
        String keyType = queueType.equals("wait") ? USER_QUEUE_WAIT_KEY : USER_QUEUE_PROCEED_KEY;
        return reactiveRedisTemplate.opsForZSet()
                .rank(keyType.formatted(queue), email)
                .defaultIfEmpty(-1L) // 아직 대기 완료 대기열에 없다면 -1를 return
                .map(rank -> rank >= 0); // 있다면 rank를 return
    }

    // 대기열에서 사용자 순위 조회
    public Mono<Long> getRank(String queue, String email) {
        return reactiveRedisTemplate.opsForZSet()
                .rank(USER_QUEUE_WAIT_KEY.formatted(queue), email)
                .defaultIfEmpty(-1L) // 대기열에 없다면 -1을 return
                .flatMap(rank -> {
                    if (rank == -1) {
                        return Mono.error(new CustomException(ErrorCode.USER_NOT_IN_QUEUE));
                    }
                    return Mono.just(rank + 1);
                })
                .doOnSuccess(rank -> {
                    log.info("사용자 {}의 순위: {} ", email, rank);
                });
    }

    // 유저를 입장 허용 큐로 이동시킴
    // 대기열queue에서 입장허용 queue로 유저를 옮기는 함수 ( 3명씩 )
    // sqs에서 메세지가 오면 트리거되서 작동됨
    @SqsListener(value = "queue")
    public void moveUsersToAllowedQueue(Acknowledgement acknowledgement) {
        // 허용할 유저 수 (3명)
        var maxAllowUserCount = 10L;

        // 대기열에서 유저를 스캔하여 입장 허용 큐로 이동
        reactiveRedisTemplate.scan(ScanOptions.scanOptions()
                        .match(USER_QUEUE_WAIT_KEY_FOR_SCAN) // 모든 대기열 확인
                        .count(100) // key를 100개 뽑음
                        .build())
                .map(key -> key.split(":")[2]) // 축제+id 형태의 redis key 2번째 값 추출 ("users:queue:festival15:wait" 에서 'festival15' 추출)
                .flatMap(queue -> allowUserWithLock(queue, maxAllowUserCount).map(allowed -> Tuples.of(queue, allowed))
                        .doOnNext(tuple -> log.info("Tried %d and allowed %d members of %s queue".formatted(maxAllowUserCount, tuple.getT2(), tuple.getT1()))))
                .doFinally(signalType -> {
                    // 처리 완료 후 ACK 전송
                    if (signalType != SignalType.ON_ERROR) {
                        log.info("완료");
                        acknowledgement.acknowledge();  // 큐 제거
                    }
                })
                .subscribe();
    }

    // 대기열에서 대기 중인 사용자를 꺼낸 후 , 대기 완료 queue에 insert
    // 두 번째 매개변수의 count 수 만큼의 유저의 수를 대기열에 먼저 들어온 순으로 pop 한다음 proccedQueue에 insert
    // insert된 유저 count를 return
    public Mono<Long> allowUser(String queue, Long count) {
        return reactiveRedisTemplate.opsForZSet()
                .range(USER_QUEUE_WAIT_KEY.formatted(queue), Range.closed(0L, count - 1))
                .collectList()
                .flatMap(users -> {
                    if (users.isEmpty()) {
                        return Mono.just(0L);
                    }

                    String[] membersArray = users.toArray(new String[0]);
                    // 대기열에서 제거 & 대기 완료열에 추가
                    return reactiveRedisTemplate.opsForZSet()
                            .remove(USER_QUEUE_WAIT_KEY.formatted(queue), (Object[]) membersArray)
                            .thenMany(Flux.fromIterable(users))
                            .flatMap(user -> reactiveRedisTemplate.opsForZSet()
                                    .add(USER_QUEUE_PROCEED_KEY.formatted(queue), user, Instant.now().toEpochMilli()))
                            .then(Mono.just((long) users.size()));
                });
    }

    // 분산락 적용
    public Mono<Long> allowUserWithLock(String queue, Long count) {
        String lockKey = "LOCK:" + queue;
        RLock rLock = redissonClient.getLock(lockKey);

        RFuture<Boolean> rFuture = rLock.tryLockAsync(3, 5, TimeUnit.SECONDS);
        return Mono.fromFuture(rFuture.toCompletableFuture())
                .flatMap(isLocked -> {
                    if (!isLocked) {
                        return Mono.error(new CustomException(ErrorCode.LOCK_ACQUISITION_FAILED));
                    }
                    log.info("락 획득 성공: {}", lockKey);
                    return allowUser(queue, count)
                            .doFinally(signal -> {
                                log.info("락 해제 시도: {}", lockKey);
                                rLock.unlockAsync()
                                        .exceptionally(ex -> {
                                            log.error("락 해제 실패", ex);
                                            return null;
                                        });
                            });
                });

    }

    // 페이지 이탈시 대기열에서 삭제
    public Mono<Void> removeUserFromQueue(String queue, String queueType, String email) {
        String keyType = queueType.equals("wait") ? USER_QUEUE_WAIT_KEY : USER_QUEUE_PROCEED_KEY;

        return reactiveRedisTemplate.opsForZSet()
                .remove(keyType.formatted(queue), email)
                .doOnSuccess(count -> log.info("삭제된 유저 수: {}", count))
                .then(); // Mono<Void> 반환
    }
}