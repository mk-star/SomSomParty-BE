package com.acc.somsomparty.domain.Reservation.service;

import com.acc.somsomparty.domain.Reservation.converter.ReservationConverter;
import com.acc.somsomparty.domain.Reservation.dto.ReservationRequestDTO;
import com.acc.somsomparty.domain.Reservation.entity.Reservation;
import com.acc.somsomparty.domain.Reservation.listener.MessageProducer;
import com.acc.somsomparty.domain.Reservation.listener.ReservationEvent;
import com.acc.somsomparty.domain.Reservation.exception.SoldOutException;
import com.acc.somsomparty.domain.Reservation.exception.TransientException;
import com.acc.somsomparty.domain.Reservation.repository.ReservationRepository;
import com.acc.somsomparty.domain.Ticket.repository.TicketRepository;
import com.acc.somsomparty.global.exception.CustomException;
import com.acc.somsomparty.global.exception.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationCommandServiceImpl implements ReservationCommandService{
    private final TicketRepository ticketRepository;
    private final ReservationRepository reservationRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final MessageProducer messageProducer;


    // --- 상수 정의 ---
    private static final String RESERVATION_TICKET_KEY = "{ticket:%s}";
    private static final String RESERVATION_FESTIVAL_KEY = "{festival:%s}";

    @Override
    public void requestReservation(ReservationRequestDTO request) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/check_ticket.lua")));
        script.setResultType(Long.class);

        String key = RESERVATION_TICKET_KEY.formatted(request.ticketId());
        Long result = redisTemplate.execute(script, List.of(key));
        if (result == null || result == 0) {
            throw new CustomException(ErrorCode.TICKET_SOLD_OUT);
        }

        // 비동기 처리
        ReservationEvent event = ReservationEvent.create(request.userId(), request.ticketId());
        messageProducer.sendMessage(event);
    }

    @Override
    public void makeReservation(ReservationEvent event) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/decrement_ticket.lua")));
        script.setResultType(Long.class);

        // 티켓 수 감소
        String key = RESERVATION_TICKET_KEY.formatted(event.ticketId());
        Long result = redisTemplate.execute(script, List.of(key));
        if (result == null || result == 0) {
            throw new SoldOutException("TICKET_SOLD_OUT");
        }

        Reservation reservation = ReservationConverter.toReservation(event);

        log.info("Reservation saved for userId={}, ticketId={}", event.userId(), event.ticketId());

        try {
            reservationRepository.save(reservation);
        } catch (DataAccessException e) {
            throw new TransientException("DB FAILED", e); // 재시도 할 예외
        }
    }

}
