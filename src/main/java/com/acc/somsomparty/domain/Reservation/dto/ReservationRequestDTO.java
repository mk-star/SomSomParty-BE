package com.acc.somsomparty.domain.Reservation.dto;

import lombok.Builder;

@Builder
public record ReservationRequestDTO(
        Long userId,
        Long ticketId)
{
}