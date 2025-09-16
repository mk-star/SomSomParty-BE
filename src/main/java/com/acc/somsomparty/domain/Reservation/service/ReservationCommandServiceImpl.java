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
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationCommandServiceImpl implements ReservationCommandService{
    private final TicketRepository ticketRepository;
    private final ReservationRepository reservationRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public ReservationResponseDTO.makeReservationResultDTO makeReservation() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/ticket.lua")));
        script.setResultType(Long.class);

        String key = "{ticket}";
        Long result = redisTemplate.execute(script, List.of(key));

        System.out.println(redisTemplate.opsForValue().get(key));
        if (result == null || result == 0) {
            throw new CustomException(ErrorCode.TICKET_SOLD_OUT);
        }

        Ticket ticket = ticketRepository.findById(1L)
                .orElseThrow(() -> new CustomException(ErrorCode.TICKET_NOT_FOUND));

        Reservation reservation = ReservationConverter.toReservation(ticket);
        reservationRepository.saveAndFlush(reservation);

       return ReservationConverter.makeReservationResultDTO(reservation);
    }

//    @Override
//    @DistributedLock(key = "'festival'")
//    public ReservationResponseDTO.makeReservationResultDTO makeReservation() {
//        Ticket ticket = ticketRepository.findById(1L).orElseThrow(() -> new CustomException(ErrorCode.TICKET_NOT_FOUND));
//
//        String key = "{ticket}";
//        Long left = redisTemplate.opsForValue().decrement(key);
//        if (left != null && left >= 0) {
////            redisTemplate.opsForValue().set(key, String.valueOf(left - 1));
//            //ticketRepository.save(ticket);
//            Reservation reservation = ReservationConverter.toReservation(ticket);
//            reservationRepository.saveAndFlush(reservation);
//            return ReservationConverter.makeReservationResultDTO(reservation);
//        }
//        // 만약 남은 티켓 수가 0이면 예외를 던짐
//        throw new CustomException(ErrorCode.TICKET_SOLD_OUT);
//    }
}
