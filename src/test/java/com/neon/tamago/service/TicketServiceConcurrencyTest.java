package com.neon.tamago.service;

import com.neon.tamago.exception.SoldOutException;
import com.neon.tamago.model.Event;
import com.neon.tamago.model.EventType;
import com.neon.tamago.model.TicketCategory;
import com.neon.tamago.repository.EventRepository;
import com.neon.tamago.repository.TicketCategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TicketServiceConcurrencyTest {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketCategoryRepository ticketCategoryRepository;

    @Autowired
    private EventRepository eventRepository;

    private TicketCategory ticketCategory;

    @BeforeEach
    void setUp() {
        // 테스트용 이벤트 및 티켓 카테고리 생성
        Event event = new Event(
                "Concurrency Test Event",
                "Testing concurrency",
                "http://example.com/image.png",
                "<p>Detail Content</p>",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(3),
                EventType.ONLINE,
                1L
        );
        eventRepository.save(event);

        ticketCategory = new TicketCategory(
                "Test Category",
                "For concurrency testing",
                BigDecimal.valueOf(100),
                10 // 총 10장의 티켓
        );
        ticketCategory.setEvent(event);
        ticketCategoryRepository.save(ticketCategory);
    }

    @Test
    @Rollback(false)
    void reserveTicket_concurrent() throws InterruptedException {
        int numberOfThreads = 20;
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger soldOutCount = new AtomicInteger();

        for (int i = 0; i < numberOfThreads; i++) {
            final Long userId = (long) i;
            new Thread(() -> {
                try {
                    ticketService.reserveTicket(ticketCategory.getId(), userId);
                    successCount.incrementAndGet();
                } catch (SoldOutException e) {
                    soldOutCount.incrementAndGet();
                } catch (Exception e) {
                    // e.printStackTrace(); // 불필요한 출력문 제거
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();

        assertEquals(10, successCount.get());
        assertEquals(10, soldOutCount.get());
    }
}