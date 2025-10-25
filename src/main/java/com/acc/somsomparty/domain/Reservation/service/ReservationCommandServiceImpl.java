package com.acc.somsomparty.domain.Reservation.service;

import com.acc.somsomparty.domain.Reservation.converter.ReservationConverter;
import com.acc.somsomparty.domain.Reservation.dto.ReservationRequestDTO;
import com.acc.somsomparty.domain.Reservation.entity.Reservation;
import com.acc.somsomparty.domain.Reservation.kafka.QueueProducer;
import com.acc.somsomparty.domain.Reservation.kafka.event.ReservationEvent;
import com.acc.somsomparty.domain.Reservation.repository.ReservationRepository;
import com.acc.somsomparty.domain.Ticket.entity.Ticket;
import com.acc.somsomparty.domain.Ticket.repository.TicketRepository;
import com.acc.somsomparty.global.exception.CustomException;
import com.acc.somsomparty.global.exception.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
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
    private final QueueProducer queueProducer;

    // --- 상수 정의 ---
    private static final String RESERVATION_TICKET_KEY = "{ticket:%s}";
    private static final String RESERVATION_FESTIVAL_KEY = "{festival:%s}";

    @Override
    public void reserve(ReservationRequestDTO request) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/stock_decrement.lua")));
        script.setResultType(Long.class);

        String key = RESERVATION_TICKET_KEY.formatted(request.ticketId());

        Long result = redisTemplate.execute(script, List.of(key));

        if (result == null || result == 0) {
            throw new CustomException(ErrorCode.TICKET_SOLD_OUT);
        }
        
        // 비동기 처리
        ReservationEvent event = ReservationEvent.create(request.userId(), request.festivalId(), request.ticketId());
        queueProducer.publishEvent(event);
    }

    @Override
    public void makeReservation(ReservationEvent event) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/rate_limit.lua")));
        script.setResultType(Long.class);

        // 티켓 재고 키 (KEYS[1]에 해당)
        String key = RESERVATION_FESTIVAL_KEY.formatted(event.festivalId());

        Long result = redisTemplate.execute(
                script,
                List.of(key),         // KEYS[1]
                "1", "1000"     // ARGV[1] = TTL, ARGV[2] = 최대 요청
        );

        // 5. 제한 초과 체크
        if (result == 0) {
            // 한도 초과: DB INSERT 대신 큐에 넣어 대기
            queueProducer.publishEvent(event);
            return;
        }

        Reservation reservation = ReservationConverter.toReservation(event);

        log.info("Reservation saved for userId={}, ticketId={}", event.userId(), event.ticketId());
        reservationRepository.save(reservation);
    }
}
