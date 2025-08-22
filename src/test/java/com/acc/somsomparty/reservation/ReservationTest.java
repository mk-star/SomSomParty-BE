package com.acc.somsomparty.reservation;

import com.acc.somsomparty.domain.Reservation.dto.ReservationRequestDTO;
import com.acc.somsomparty.domain.Reservation.service.ReservationCommandService;
import com.acc.somsomparty.domain.Ticket.entity.Ticket;
import com.acc.somsomparty.domain.Ticket.repository.TicketRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class ReservationTest {
    @Autowired
    private ReservationCommandService reservationCommandService;

    @Autowired
    private TicketRepository ticketRepository;

    @Test
    public void 동시_80개의_요청() throws InterruptedException {

        int threadCount = 75;
        //비동기로 실행되는 작업을 단순화해서 사용하게 도와주는 자바의 api
        ExecutorService executorService = Executors.newFixedThreadPool(32);

        //카운트다운래치는 다른 스래드에서 수행중인 작업을 완료될때까지 대기하도록 도와주는 클래스
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            ReservationRequestDTO.makeReservationDTO request = ReservationRequestDTO.makeReservationDTO.builder()
                    .festivalId(1L)
                    .festivalDate(LocalDate.parse("2025-08-22"))
                    .build();

            executorService.submit(() -> {
                try {
                    reservationCommandService.makeReservation(request);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executorService.shutdown();

        //검증 프로세스 진행~~
        Ticket ticket = ticketRepository.findByFestivalIdAndFestivalDate(1L, LocalDate.parse("2025-08-22"))
                .orElseThrow();
        assertEquals(0, ticket.getLeftTickets(), "C 모두 소진되어야 함");
    }
}
