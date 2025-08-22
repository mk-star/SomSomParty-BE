package com.acc.somsomparty.domain.Reservation.service;

import com.acc.somsomparty.domain.Reservation.dto.ReservationRequestDTO;
import com.acc.somsomparty.domain.Reservation.dto.ReservationResponseDTO;

public interface ReservationCommandService {
    ReservationResponseDTO.makeReservationResultDTO makeReservation(ReservationRequestDTO.makeReservationDTO request);
}
