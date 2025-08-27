# 🎉 축제 예매 플랫폼 '솜솜파티'

## 프로젝트 소개
사용자가 축제를 예약하고, 참여자들과 정보를 공유하며 실시간으로 소통할 수 있는 종합 축제 플랫폼

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

## 담당 업무 (풀스택)
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
### Kafka + Redis Sorted Set를 이용한 대기열 시스템

- 대기열 시스템은 클라이언트가 요청을 보낼 때 이를 차례대로 처리함으로써 서버 과부하를 방지하고 안정적인 서비스를 제공
- 특히, 티켓팅과 같이 특정 시간에 요청이 집중되는 상황에서는 대기열 시스템이 필수적이며, 이를 효율적으로 구현하기 위해 **Kafka**와 **Redis**를 함께 활용

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
- Value: 사용자 ID
- Score: 대기열에 입장한 시간을 유닉스타임(m/s) 값으로 설정

<br/>

**Kafka**
- 대기열 진입 메시지를 비동기적으로 처리하고
- 파티션을 통해 동시에 여러 작업을 수행하여 대기열 효율성을 높임
<img src="https://github.com/user-attachments/assets/f82678d5-8376-41ba-9f04-73b5d351e445">

<br/>

대기열 시스템을 통해 위와 같이 사용자들이 자신의 현재 대기 번호를 실시간으로 확인 가능


<br/>


### 요청 흐름

## 예약 기능

### Redis 분산 락 (Distribution Lock) 적용
```java
    @Override
    @DistributedLock(key = "'festival-' + #request.festivalId + '-' + #request.festivalDate")
    public ReservationResponseDTO.makeReservationResultDTO makeReservation(ReservationRequestDTO.makeReservationDTO request) {
        // 생략

        if(ticket.getLeftTickets() >= 1) {
            ticket.setLeftTickets(ticket.getLeftTickets() - 1);
            ticketRepository.save(ticket);
            Reservation reservation = ReservationConverter.toReservation(ticket);
            reservationRepository.saveAndFlush(reservation);
            return ReservationConverter.makeReservationResultDTO(reservation);
        }
        // 생략
    }
```
- Redis 분산 락 적용으로 여러 서버에서 동시에 예약 요청이 들어와도 티켓 수 감소 연산의 정합성 유지
- @DistributedLock 어노테이션을 사용해 특정 축제와 날짜 기준으로 안전하게 동시성 제어

<br/>

<img src="https://github.com/user-attachments/assets/35a4e982-ffe3-4267-bdcf-31314f5c5919">

<br/>
