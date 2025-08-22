package com.acc.somsomparty.domain.Ticket.repository;

import com.acc.somsomparty.domain.Ticket.entity.Ticket;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Optional<Ticket> findByFestivalIdAndFestivalDate(@Param("festivalId") Long festivalId, @Param("festivalDate") LocalDate festivalDate);
}
