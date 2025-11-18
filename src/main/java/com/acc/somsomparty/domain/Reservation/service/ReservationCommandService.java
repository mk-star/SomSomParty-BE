package com.acc.somsomparty.domain.Reservation.service;

import com.acc.somsomparty.domain.Reservation.dto.ReservationRequestDTO;

import com.acc.somsomparty.domain.Reservation.listener.ReservationEvent;

public interface ReservationCommandService {
    void requestReservation(ReservationRequestDTO request);
    void makeReservation(ReservationEvent event);
}
