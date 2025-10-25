package com.acc.somsomparty.domain.Reservation.kafka.topics;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EventTopic {
    RESERVATION_COMPLETED("reservation-completed");

    private final String topic;
}
