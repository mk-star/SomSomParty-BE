package com.acc.somsomparty.domain.Reservation.kafka.event;

public record ReservationEvent(
        Long userId,
        Long festivalId,
        Long ticketId
) {
    public static ReservationEvent create(Long userId, Long festivalId, Long ticketId) {
        return new ReservationEvent(
                userId,
                festivalId,
                ticketId
        );
    }
}
