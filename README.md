# 🎉 대용량 트래픽을 처리하는 축제 예매 플랫폼

## 프로젝트 소개
사용자가 축제를 쉽고 직관적으로 예약하고, 참여자들과 정보를 공유하며 실시간 소통할 수 있는 종합 축제 예약 플랫폼

<br/>

## 주요 기능
### 🎪 축제 정보 제공
축제 이름, 시작일과 종료일, 상세 설명 등 축제 관련 정보를 한눈에 확인할 수 있습니다.
### 📝 축제 예약 
원하는 축제를 쉽고 빠르게 예약할 수 있으며, 대기열 페이지를 통해 실시간 예약 순서 확인이 가능합니다.
### 💬 채팅 시스템
실시간으로 소통하며 정보를 공유할 수 있습니다.
### 🔍 축제 검색
최신 축제 정보를 탐색하거나, 사용자가 입력한 키워드로 원하는 축제를 빠르게 검색할 수 있습니다.
### 🔔 축제 알림 
예약한 축제 시작 하루 전, 푸시 알림을 통해 사용자에게 알려줍니다.

<br/>

## 기술 스택
- Tech Stack
  - Spring Boot, Spring JPA, AWS Cognito, Apache Kafka, Firebase Cloud Messaging, AWS EventBridge, AWS SNS, AWS SQS
- DB
  - AWS RDS (MySQL), AWS ElastiCache, DynamoDB
- DevOps
  - AWS EC2, AWS Application Load Balancer, AWS Code Deploy, Github Actions, Docker

<br/>

## 담당 업무
- 프론트엔드 배포
- 대기열 시스템 구현
- 축제 예매 기능 구현

<br/>

## ERD
![image](https://github.com/user-attachments/assets/ce39f713-489f-46b5-b8aa-b778a05ecf27)

<br/>

## CI/CD - Front-End
![image](https://github.com/user-attachments/assets/dd198a96-5e3e-45f3-abce-e863d62940e5)
- 정적 파일(S3 + CloudFront)은 CI/CD 파이프라인에서 GitHub Actions를 활용해 자동으로 S3에 업로드하고, CloudFront의 캐시를 무효화하는 방식으로 무중단 배포를 구현
- 백엔드 서버(다른 origin)로 요청을 날릴 수 없기 때문에, Origins 설정에 ACM을 적용한 Application Load Balancer(이하 ALB)를 연결해 주고 요청은 ALB를 통해 EC2로 전달하도록 설정하여 Mixed Content 에러 해결

<br/>

## 대기열 시스템
### AWS SQS + Redis를 이용한 대기열 시스템

- 대기열 시스템은 클라이언트가 요청을 보낼 때 이를 차례대로 처리함으로써 서버 과부하를 방지하고 안정적인 서비스를 제공
- 특히, 티켓팅과 같이 특정 시간에 요청이 집중되는 상황에서는 대기열 시스템이 필수적이며, 이를 효율적으로 구현하기 위해 **AWS SQS**와 **Redis**를 함께 활용

<br/>

**Redis** 
- 실시간 대기열 순서 관리
- 유저들의 대기 상태 관리

<br/>

**Redis Sorted Set이란?**
- Sorted Set은 key 하나에 여러 개의 score와 value로 구성되는 자료구조
- value는 score로 sort되며 중복되지 않음

<br/>

**프로젝트 설정**
- Key: Festival 정보
- Value: 사용자 이메일
- Score: 대기열에 입장한 시간을 유닉스타임(m/s) 값으로 설정

<br/>

**SQS**
- 대기 처리와 관련된 메시지를 비동기적으로 처리
- 대기열의 상태 변화를 안정적이고 효율적으로 관리
<img src="https://github.com/user-attachments/assets/f82678d5-8376-41ba-9f04-73b5d351e445">

<br/>

대기열 시스템을 통해 위와 같이 사용자들이 자신의 현재 대기 번호를 실시간으로 확인 가능


<br/>


### 요청 흐름

<b>1. 유저가 대기열에 있는 경우</b>
<p align="center"><img src="https://github.com/user-attachments/assets/5b1a5b66-a5f4-49a7-8fa3-b8f339f20fc0"></p>

<br/>

- 최초 요청 시:
    - 유저는 Redis의 Sorted Set을 이용한 대기열에 등록됨
    - 이때 대기열에 유저가 성공적으로 등록되면, 해당 유저는 대기열 내에서 순위가 부여됨
    - `SqsSender`의 `send()` 메서드를 통해 SQS로 대기열에 유저가 등록되었다는 메시지를 전달
- 재요청 시:
    - 유저가 재요청을 하면, 먼저 해당 유저가 대기열에 있는지 확인하고, 대기열에 있다면  대기표(대기 순위)를 반환
- 대기열에서 유저 스캔 및 입장 허용:
    - 일정 시간(SQS의 지연 시간)이 지난 후, SQS로부터 응답이 오면 대기열에서 유저들을 스캔하여 순차적으로 10명씩 대기 완료 열로 이동
> 서버 부하를 줄이기 위해서 10개씩 발급

<br/>

<b>2. 유저가 대기완료 열로 이동한 경우</b>
<p align="center"><img src="https://github.com/user-attachments/assets/cacbdf16-7386-4e37-8d46-6cac8d509fac"></p>
   
<br/>

- 재요청 시:
    - 유저가 대기열에 없다면, `-1`이 반환되어 유저는 대기열에 없는 상태로 처리
    - `-1`을 받은 유저는 대기완료 열에 유저의 존재 여부를 다시 확인하기 위한 요청을 보냄
    - 대기완료 큐에 유저가 존재한다면, 해당 유저는 대기열을 통과했음을 의미하므로 예약 페이지로 이동하여 예약 진행 가능

<br/>

### 부하 테스트
> k6

- 최대 사용자: 1000명
- 램프 업: 1000명의 사용자에 도달할 때까지 30초마다 100명의 사용자 추가
- 테스트 시나리오: 대기열 입장 → 5초 마다 rank 응답 반환 → 대기완료 열로 이동 → 타겟 페이지로 이동 후 대기완료 열에서 제거(사용자 점차 감소)

<img src="https://github.com/user-attachments/assets/325a1cc4-141e-418a-9f63-9eb4e8a7e4b7">

<br/>

- 결과
    - 평균 응답 시간: 1.28s
        - 응답 시간이 9.55s까지 늘어날 수 있다는 점이 있음
    - 요청 실패율: 0%
    - 지연 시간 = 1.28초 - 1.27초 = 0.01초
        - 네트워크 지연 등을 포함한 최대 시간으로, 0.01초로 짧은 것으로 나타남
    - 처리량 = 56,045 / 308.8 ≈ 181.48 RPS
        - 낮은 처리량 개선 필요 - 불필요한 반복 요청 줄이기(Polling 간격을 1초 → 3~5초로 늘리기), 서버 성능 최적화 필요

<br/>

## 예매 기능

### 비관적 락(Pessimistic Lock) 적용
```java
@Override
@Transactional
public ReservationResponseDTO.makeReservationResultDTO makeReservation(Long userId, ReservationRequestDTO.makeReservationDTO request) {
    Ticket ticket = ticketRepository.findByFestivalIdAndFestivalDateWithLock(request.getFestivalId(), request.getFestivalDate()).orElseThrow(() -> new CustomException(ErrorCode.TICKET_NOT_FOUND));
}

// ------------------------------------------------------------

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Ticket t WHERE t.festival.id = :festivalId AND t.festivalDate = :festivalDate")
    Optional<Ticket> findByFestivalIdAndFestivalDateWithLock(@Param("festivalId") Long festivalId,
                                                             @Param("festivalDate") LocalDate festivalDate);
}
```
- 공유 자원인 Ticket를 `TicketRepository`에서 불러올 때, `@Lock` 어노테이션을 사용하여 비관적 쓰기 락(PESSIMISTIC_WRITE LOCK)을 적용
- 이를 통해 한 트랜잭션이 이 Ticket를 읽고 수정하는 동안 다른 트랜잭션이 접근하지 못하게 하여 동시성 문제를 방지하고, 데이터의 무결성을 유지

<br/>

<img src="https://github.com/user-attachments/assets/35a4e982-ffe3-4267-bdcf-31314f5c5919">

<br/>
