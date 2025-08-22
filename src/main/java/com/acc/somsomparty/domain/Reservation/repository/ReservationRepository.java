package com.acc.somsomparty.domain.Reservation.repository;

import com.acc.somsomparty.domain.Reservation.entity.Reservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
//    Page<Reservation> findAllByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
//    Page<Reservation> findByUserIdAndCreatedAtLessThanOrderByCreatedAtDesc(Long userId, LocalDateTime createdAt, Pageable pageable);
}
