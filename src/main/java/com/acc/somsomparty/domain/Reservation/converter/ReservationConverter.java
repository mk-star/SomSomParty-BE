package com.acc.somsomparty.domain.Reservation.converter;

import com.acc.somsomparty.domain.Festival.converter.FestivalConverter;
import com.acc.somsomparty.domain.Reservation.dto.ReservationResponseDTO;
import com.acc.somsomparty.domain.Reservation.entity.Reservation;
import com.acc.somsomparty.domain.Ticket.entity.Ticket;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ReservationConverter {
    public static ReservationResponseDTO.ReservationPreViewDTO reservationPreViewDTO(Reservation reservation) {
        return ReservationResponseDTO.ReservationPreViewDTO.builder()
                .id(reservation.getId())
                .reservationDate(reservation.getReservationDate())
                .festivalDate(reservation.getTicket().getFestivalDate())
                .festivalInfo(FestivalConverter.festivalPreViewDTO(reservation.getTicket().getFestival()))
                .build();
    }

    public static ReservationResponseDTO.ReservationPreViewListDTO reservationPreViewListDTO(List<ReservationResponseDTO.ReservationPreViewDTO> reservations, boolean hasNext, Long lastId) {
        return ReservationResponseDTO.ReservationPreViewListDTO.builder()
                .reservations(reservations)
                .hasNext(hasNext)
                .lastId(lastId)
                .build();
    }

    public static Reservation toReservation(Ticket ticket) {
        return Reservation.builder()
                .reservationDate(LocalDate.now())
                .ticket(ticket)
                .build();
    }

    public static ReservationResponseDTO.makeReservationResultDTO makeReservationResultDTO(Reservation reservation) {
        return ReservationResponseDTO.makeReservationResultDTO.builder()
                .reservationId(reservation.getId())
                .createdAt(LocalDateTime.now())
                .build();
    }
}
