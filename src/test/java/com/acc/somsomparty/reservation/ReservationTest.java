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
    public void testPreReserveSeatConcurrency() throws InterruptedException {
        UUID scheduleId = UUID.fromString("8fb9facb-2a07-47f7-aed6-05f5e7928b3e");
        UUID seatId = UUID.fromString("59f33c13-7aa7-49be-9caa-767972ec12b9");
        UUID userId = UUID.randomUUID();

        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch latch = new CountDownLatch(5);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < 5; i++) {
            executor.submit(() -> {
                try {
                    luaScriptService.preReserveSeat(scheduleId, seatId,  userId);
                    if (successCount.incrementAndGet() == 1) {
                        System.out.println("예약 성공!");
                    }
                } catch (ApplicationException e) {
                    assertEquals(e.getMessage(), "좌석이 이미 점유되어 있습니다.");
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        assertEquals(1, successCount.get(), "성공한 예약은 1번만 있어야 합니다.");
        executor.shutdown();
    }
}
