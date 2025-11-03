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
- 축제 예약 기능 구현

<br/>

## ERD
![image](https://github.com/user-attachments/assets/ce39f713-489f-46b5-b8aa-b778a05ecf27)

<br/>

## CI/CD - Front-End
![image](https://github.com/user-attachments/assets/dd198a96-5e3e-45f3-abce-e863d62940e5)
- 정적 파일(S3 + CloudFront)은 CI/CD 파이프라인에서 GitHub Actions를 활용해 자동으로 S3에 업로드하고, CloudFront의 캐시를 무효화하는 방식으로 무중단 배포를 구현
- 백엔드 서버(다른 origin)로 요청을 날릴 수 없기 때문에, Origins 설정에 ACM을 적용한 Application Load Balancer(이하 ALB)를 연결해 주고 요청은 ALB를 통해 EC2로 전달하도록 설정하여 Mixed Content 에러 해결

<br/>


## 예약 기능

### Kafka Cluster
- 파티션 기반 병렬 처리로 동시에 몰리는 대규모 예약 요청을 빠르게 처리
- 수평 확장을 통해 예약 트래픽 급증 상황에도 유연하게 대응
- 데이터 유실을 최소화하여 안정적이고 신뢰성 있는 예약 서비스 제공

### Redis Lua Script 적용
```java
local stock = redis.call('GET', KEYS[1])
if tonumber(stock) <= 0 then
    return 0
else
    redis.call('DECR', KEYS[1])
    return 1
end
```
- 분산 서버 환경에서 티켓 예약 시 발생할 수 있는 동시성 문제를 방지하기 위해 Redis Lua Script 사용
- 동시에 예약 요청이 들어와도 티켓 수 감소 연산의 정합성 유지


<br/>

<img src="https://github.com/user-attachments/assets/35a4e982-ffe3-4267-bdcf-31314f5c5919">

<br/>
