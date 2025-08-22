package com.acc.somsomparty.domain.Reservation.service;

import com.acc.somsomparty.domain.Reservation.dto.ReservationResponseDTO;

public interface ReservationQueryService {
    ReservationResponseDTO.ReservationPreViewListDTO getReservationList(Long userId, Long lastId, int offset);
}
