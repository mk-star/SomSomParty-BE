package com.acc.somsomparty.domain.Reservation.dto;

import com.acc.somsomparty.domain.Festival.dto.FestivalResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ReservationResponseDTO {
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReservationPreViewDTO {
        Long id;
        LocalDate reservationDate;
        LocalDate festivalDate;
        FestivalResponseDTO.FestivalPreViewDTO festivalInfo;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReservationPreViewListDTO {
        List<ReservationPreViewDTO> reservations;
        boolean hasNext;
        Long lastId;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class makeReservationResultDTO {
        Long reservationId;
        LocalDateTime createdAt;
    }
}