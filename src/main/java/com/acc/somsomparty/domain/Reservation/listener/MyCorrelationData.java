package com.acc.somsomparty.domain.Reservation.listener;

import org.springframework.amqp.rabbit.connection.CorrelationData;

public class MyCorrelationData extends CorrelationData {

    //  원본 이벤트 객체를 저장할 필드
    private final ReservationEvent event;

    public MyCorrelationData(String id, ReservationEvent event) {
        super(id); // 부모 클래스에 ID 전달
        this.event = event;
    }

    // 콜백에서 원본 이벤트를 꺼내기 위한 Getter
    public ReservationEvent getEvent() {
        return event;
    }

    @Override
    public String toString() {
        return "MyCorrelationData [id=" + getId() + ", event=" + event + "]";
    }

}