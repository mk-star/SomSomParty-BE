package com.acc.somsomparty.domain.Reservation.listener;

public record ReservationEvent(
        Long userId,
        Long ticketId
) {
    public static ReservationEvent create(Long userId, Long ticketId) {
        return new ReservationEvent(
                userId,
                ticketId
        );
    }
}
