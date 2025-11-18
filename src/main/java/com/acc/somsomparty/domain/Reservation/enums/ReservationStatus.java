package com.acc.somsomparty.domain.Reservation.enums;

public enum ReservationStatus {
    PENDING,    // 요청 접수됨, 아직 처리 전
    CONFIRMED,  // 성공적으로 DB 저장 완료
    FAILED,     // 처리 실패
    CANCELLED   // 취소됨
}
