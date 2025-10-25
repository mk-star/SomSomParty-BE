package com.acc.somsomparty.domain.Reservation.repository;

import com.acc.somsomparty.domain.Reservation.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
}
