package com.acc.somsomparty.domain.Reservation.service;

import com.acc.somsomparty.domain.Reservation.aop.DistributedLock;
import com.acc.somsomparty.domain.Reservation.converter.ReservationConverter;
import com.acc.somsomparty.domain.Reservation.dto.ReservationRequestDTO;
import com.acc.somsomparty.domain.Reservation.dto.ReservationResponseDTO;
import com.acc.somsomparty.domain.Reservation.entity.Reservation;
import com.acc.somsomparty.domain.Reservation.repository.ReservationRepository;
import com.acc.somsomparty.domain.Ticket.entity.Ticket;
import com.acc.somsomparty.domain.Ticket.repository.TicketRepository;
import com.acc.somsomparty.global.exception.CustomException;
import com.acc.somsomparty.global.exception.error.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReservationCommandServiceImpl implements ReservationCommandService{
    private final TicketRepository ticketRepository;
    private final ReservationRepository reservationRepository;

    @Override
    @DistributedLock(key = "'festival-' + #request.festivalId + '-' + #request.festivalDate")
    public ReservationResponseDTO.makeReservationResultDTO makeReservation(ReservationRequestDTO.makeReservationDTO request) {
        Ticket ticket = ticketRepository.findByFestivalIdAndFestivalDate(request.getFestivalId(), request.getFestivalDate()).orElseThrow(() -> new CustomException(ErrorCode.TICKET_NOT_FOUND));

        if(ticket.getLeftTickets() >= 1) {
            ticket.setLeftTickets(ticket.getLeftTickets() - 1);
            ticketRepository.save(ticket);
            Reservation reservation = ReservationConverter.toReservation(ticket);
            reservationRepository.saveAndFlush(reservation);
            return ReservationConverter.makeReservationResultDTO(reservation);
        }
        // 만약 남은 티켓 수가 0이면 예외를 던짐
        throw new CustomException(ErrorCode.TICKET_SOLD_OUT);
    }
}
