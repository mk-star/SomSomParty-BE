package com.acc.somsomparty.domain.Reservation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

public class ReservationRequestDTO {
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class makeReservationDTO {
        Long festivalId;
        LocalDate festivalDate;
    }
}
