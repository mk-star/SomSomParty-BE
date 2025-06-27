# 서비스 소개
사용자가 축제를 쉽고 직관적으로 예약하고, 참여자들과 정보를 공유하며 실시간 소통할 수 있는 종합 축제 예약 플랫폼


<br/>


## 서비스 주요 기능
1. 회원가입, 로그인
   - 기본 정보(이메일, 비밀번호, 이름)를 입력하여 간편하게 회원가입할 수 있으며, 가입한 계정을 통해 서비스 이용 가능
2. 축제 정보 제공
    - 축제 이름, 시작일과 종료일, 상세 설명 등 축제에 대한 전반적인 정보를 한눈에 확인할 수 있도록 제공
3. 축제 예약 
    - 원하는 축제를 쉽고 빠르게 예약할 수 있으며, 대기열 페이지를 통해 실시간으로 예약 순서를 확인할 수 있도록 구성
4. 채팅 시스템
    - 실시간으로 소통하며 정보를 공유할 수 있는 채팅 기능을 제공
5. 축제 검색
    - 최신 축제 정보를 탐색하거나 사용자가 입력한 키워드를 통해 원하는 축제를 빠르게 검색할 수 있도록 지원
6. 마이페이지
    - 사용자가 예약한 축제와 참여 중인 채팅방을 확인하고 관리할 수 있도록 지원
7. 축제 알림 
    - 예약한 축제가 시작되기 하루 전에 푸시 알림을 제공


<br/>


## 기술 스택
- Tech Stack
  - Spring Boot, Spring JPA, Spring Security, JWT, AWS Cognito, AWS SQS, Kafka, Stomp, Firebase Cloud Messaging, AWS EventBridge, AWS SNS, AWS SQS
- DB
  - AWS RDS(MySQL), AWS ElastiCache, DynamoDB
- DevOps
  - AWS EC2, AWS Application Load Balancer, AWS Code Deploy, Github Actions, Docker


<br/>


# 아키텍처 설계
![image](https://github.com/user-attachments/assets/bcd3da7f-7b91-4f23-93d5-1734c79c6f40)

- 오토스케일링 그룹과 ALB를 활용해 고가용성과 확장성을 지닌 아키텍처 구성
- EC2, RDS, ElasticCache 와 같은 리소스는 멀티 AZ 배포를 해 단일 장애점을 방지
- 데이터베이스와 브로커는 EC2와 다른 프라이빗 서브넷에 배치함으로써 EC2로만 접근이 가능해 보안이 향상


<br/>


# CI/CD
## Back-End
![image](https://github.com/user-attachments/assets/9e189e86-3002-415f-916c-6c47c2917f64)
![image](https://github.com/user-attachments/assets/06a1b010-6010-460a-b3ee-1c4803f59ffd)

- Github Action, AWS ECR, Code Deploy를 활용한 Blue/Green 무중단 배포 파이프라인 구성
- Blue/Green이란 대체 인스턴스(Green)를 생성해 배포하는 과정에서 원본 인스턴스(Blue)를 유지하고, 배포가 완료되면 트래픽을 전환한 후 Blue 그룹을 안전히 제거함으로써 배포 과정에서 서버가 중단되지 않는 배포 방식
- 여러 명이 동시에 작업 중일 때도 배포로 인해 서버가 중단되지 않아 개발 효율이 증진됨

## Front-End
![image](https://github.com/user-attachments/assets/dd198a96-5e3e-45f3-abce-e863d62940e5)
- 정적 파일(S3 + CloudFront)은 CI/CD 파이프라인에서 GitHub Actions를 활용해 자동으로 S3에 업로드하고, CloudFront의 캐시를 무효화하는 방식으로 무중단 배포를 구현
- 백엔드 서버(다른 origin)로 요청을 날릴 수 없기 때문에, Origins 설정에 ACM을 적용한 Application Load Balancer(이하 ALB)를 연결해 주고 요청은 ALB를 통해 EC2로 전달하도록 설정하여 Mixed Content 에러 해결


<br/>


# Back-End
## ERD
<img src="https://github.com/user-attachments/assets/ce39f713-489f-46b5-b8aa-b778a05ecf27" />


<br/>


## API 설계
| **기능** | API URI | HTTP Method |
| --- | --- | --- |
| 회원가입 | /signup | POST |
| 회원가입 이메일 인증 | /confirm-signup | POST |
| 로그인 | /login | POST |
| 로그아웃 | /signout | POST |
| 토큰 유효성 검사 | /verify-token | GET |
| 토큰 갱신 | /refresh-token | POST |
| 페스티벌 생성 | /festivals/create | POST |
| 페스티벌 목록 조회 | /festivals | GET |
| 페스티벌 세부 정보 조회 | /festivals/{festivalId} | GET |
| 페스티벌 검색 | /festivals/search | GET |
| 티켓 예매 | /reservations | POST |
| 내 예약 목록 조회 | /reservations | GET |
| 채팅방 참여 | /festivals/chatting/{chatRoomId}/join | POST |
| 채팅방 진입 | /festivals/chatting/{chatRoomId} | GET |
| 참여 중인 채팅방 목록 조회 | /festivals/chatting/list | GET |
| 채팅방 나가기 | /festivals/chatting/delete | DELETE |
| FCM 토큰 비활성화 | /notification/deactivate | POST |
| FCM 토큰 활성화 | /notification/activate | POST |
| 대기열 등록 | /queues/{queue}/waiting-room/users/{email} | GET |
| 대기열에서 유저 순위 반환 | /queues/{queue}/users/{email}/rank | GET |
| 예약 페이지 진입 가능 여부 반환 | /queues/{queue}/users/{email}/allowed | GET |
| 대기열 탈퇴 | /queues/{queue}/users/{email}/leave | DELETE |
| 대기완료열 탈퇴 | /queues/{queue}/users/{email}/leave-proceed | DELETE |


<br/>


## 회원가입/로그인
### AWS Cognito + Spring Security + JWT
AWS Cognito는 사용자 인증, 권한 부여, 사용자 관리를 위한 클라우드 서비스이며, 인증 토큰 발행을 통해 사용자 인증을 간편하게 처리할 수 있습니다. Cognito가 발급한 JWT 토큰을 Spring Security가 검증하도록 하여 보안 정책을 적용하였습니다.

<br/>

- 회원가입
![회원가입-로직](https://github.com/user-attachments/assets/1d30f61b-1350-4cfe-86b8-40c709dcab9b)

- 로그인
  ![로그인-로직](https://github.com/user-attachments/assets/617476b4-5d06-4368-aab4-a83dd8549ec3)

<br/>

![회원가입](https://github.com/user-attachments/assets/66f5e935-b105-4a5f-9e3f-6577d16d5c92)

<br/>

![로그인-로그아웃](https://github.com/user-attachments/assets/728c9779-9ee6-4e1f-8fe4-e3817cf18e47)


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


## 예약 기능

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


## 검색 기능: ElastiCache와 Redis를 활용한 성능 개선

### AWS ElastiCache란?
- AWS에서 제공하는 완전 관리형 캐싱 서비스
- Redis나 Memcached 같은 오픈소스 캐시 엔진을 기반으로 동작
- 데이터 접근 속도를 높이고 애플리케이션 성능을 향상시키는 데 최적화

<br/>

### 검색 동작 흐름
1. Redis에서 캐시 조회
    - 캐시된 검색 결과가 있으면 반환
2. 캐시에 검색 결과가 없는 경우
    - DB에서 검색 → 결과를 Redis에 캐싱한 뒤 반환

<br/>

### 캐시 설정 및 전략
- 캐싱 TTL(Time-To-Live): 10분
    - 최신 데이터를 유지하면서 불필요한 캐싱 비용 및 메모리 사용량을 줄이기 위함
- 캐시 무효화 전략
    - 새로운 축제 데이터 생성 시 관련 캐시를 삭제하여 DB 데이터와 캐시 데이터 간 일관성을 보장

<br/>

### 성능 테스트

- 테스트 환경
    - 100개의 축제 데이터를 대상으로 특정 키워드 검색 API를 100번 호출
- 결과
    - 캐시 적용 전: 약 0.91초
    - 캐시 적용 후: 약 0.63초
    - 성능 약 **0.28초** 개선
 
<br/>

<img src="https://github.com/user-attachments/assets/4b07ce6d-c1c4-4cc6-92af-c12d68949fb1">

<br/><br/>


## 축제 알림
### 초기 설계

> EventBridge → API 호출 → 서버 내  푸시 알림 전송


<b>단점</b> 

1. 서버 리소스 집중
    - 알림 대상자가 많아질수록 서버 부하 증가
2. 서비스 결합도
    - EventBridge가 API와 직접 연결되어 있어, API 구조 변경 시 EventBridge 설정도 수정이 필요
3. 확장성 부족
    - 다른 알림 채널(이메일, SMS 등)로 확장하려면 추가 개발 필요

<br/>

### 최종 설계

> EventBridge → SNS → SQS → SNS로 푸시 알림 전송


<b>보완된 점</b>

1. 내결함성 강화
    - SQS가 메시지를 대기열에 저장하기 때문에, 서버가 다운되더라도 메시지가 유실되지 않음
2. 서버 부하 감소
    - 알림 전송 작업을 SNS로 위임하여, SNS가 실제 알림 메시지를 전송
3. 서비스 결함도 감소
    - EventBridge와 SQS가 직접적으로 결합되지 않고, SNS가 이를 연결함
    - SNS는 다중 구독자를 지원하기 때문에 이메일, Lambda 등 다양한 채널로 메시지 전송 가능

<br/>

<img src="https://github.com/user-attachments/assets/ab2cbe4a-e505-4729-8aad-200a6cc8838b">


<br/>


# 채팅 시스템

## Kafka & Stomp & DynamoDB & Redis 기반 채팅 시스템
![image](https://github.com/user-attachments/assets/ca1763a3-d521-4525-9da0-22dbe8be6b0f)

분산 서버 환경에서 채팅 시스템의 요구사항은 다음과 같습니다. 

> 1. 실시간으로 메세지를 송수신 해야 합니다.
> 2. 메세지의 정확성을 보장 해야 합니다. 
> 3. 대규모 데이터인 메세지를 빠르게 로드 해와야 합니다.

이 3가지 조건을 각각 다음과 같은 구성으로 만족시켰습니다.

## 1. 실시간 메세지 송수신

![솜솜파티 실시간채팅GIF](https://github.com/user-attachments/assets/e7b3333c-83f3-45d8-a0e0-f50c17e7221e)


채팅에 자주 사용되는 STOMP를 사용해 구현했습니다. 

기본적으로 STOMP는 내장 메세지 브로커를 사용해 메세지 상태(순서, 채팅방 등)을 서버 메모리에 저장합니다. 그러나 분산 서버 환경에선 서버 간 구독 정보가 공유되지 않기 때문에 내장 메세지 브로커를 사용할 수 없습니다. 따라서 분산 서버 환경에선 중앙 집중 메세지 브로커가 필요합니다.

## 2. 메세지 정확성 보장 & 병렬 처리로 처리량 극대화

분산 서버 환경에서 메세지의 순서, 채팅방 등 정확성을 보장하기 위해 중앙 집중 메세지 브로커인 카프카를 도입했습니다. STOMP의 메세지 브로커 역할을 카프카가 대신함으로써 분산된 서버의 각 사용자들에게 정확하게 메세지를 전달할 수 있게 되었습니다. 또한, 카프카의 특성인 파티션과 컨슈머 그룹을 이용해 메세지를 병렬로 처리하기 때문에 대규모 트래픽을 안정적으로 처리할 수 있게 되었습니다.

## 3.  대규모 데이터 로드

![솜솜파티 무한스크롤GIF](https://github.com/user-attachments/assets/76fd8e91-3e8b-4e58-9d18-c02255ba92d0)


채팅 메세지 내역은 대규모 데이터이기 때문에 RDS만으로는 빠른 처리가 불가하다고 판단했습니다. 따라서 Key-Value 기반의 DynamoDB에 메세지 데이터를 저장하여 빠르게 데이터를 로드해오게 구현하였습니다. 

## + 추가 기능 구현

![솜솜파티 채팅알림GIF](https://github.com/user-attachments/assets/a68216a8-ae4e-4ccd-a4dc-c86b8fe222c3)

읽지 않은 메세지 알림을 구현하였습니다. 이를 위해 채팅방 온라인 유저와 오프라인 유저를 구별해야 했는데, 이는 채팅방 입장/퇴장 시마다 호출되는 메서드이어야 하므로 빠른 처리와 RDS 부하 감소가 필수였습니다. 따라서 Redis로 온라인/오프라인 유저와 알림 개수를 계산하게 구현하였습니다.

