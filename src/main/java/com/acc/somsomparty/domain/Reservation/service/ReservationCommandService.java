package com.acc.somsomparty.domain.Reservation.service;

import com.acc.somsomparty.domain.Reservation.dto.ReservationRequestDTO;

import com.acc.somsomparty.domain.Reservation.kafka.event.ReservationEvent;

public interface ReservationCommandService {
    void reserve(ReservationRequestDTO request);
    void makeReservation(ReservationEvent event);
}
