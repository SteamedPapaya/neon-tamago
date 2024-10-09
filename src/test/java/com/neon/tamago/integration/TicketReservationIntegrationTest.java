package com.neon.tamago.integration;

import com.neon.tamago.model.Event;
import com.neon.tamago.model.TicketCategory;
import com.neon.tamago.repository.EventRepository;
import com.neon.tamago.repository.TicketCategoryRepository;
import com.neon.tamago.repository.TicketRepository;
import com.neon.tamago.service.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class TicketReservationIntegrationTest {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TicketCategoryRepository ticketCategoryRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private TicketService ticketService;

    private Long ticketCategoryId;

    @BeforeEach
    public void setup() {
        // 기존 데이터 삭제
        ticketRepository.deleteAll();
        ticketCategoryRepository.deleteAll();
        eventRepository.deleteAll();

        // 테스트용 이벤트 생성
        Event event = new Event();
        event.setTitle("Test Event");
        eventRepository.saveAndFlush(event);

        // 테스트용 티켓 카테고리 생성
        TicketCategory ticketCategory = new TicketCategory();
        ticketCategory.setRemainingQuantity(10);
        ticketCategory.setEvent(event);
        ticketCategoryRepository.saveAndFlush(ticketCategory);

        // 저장된 티켓 카테고리의 ID 가져오기
        ticketCategoryId = ticketCategory.getId();

        // Redis에 남은 티켓 수 초기화
        RAtomicLong stock = redissonClient.getAtomicLong("stock:ticketCategory:" + ticketCategoryId);
        stock.set(10);
    }

    @Test
    public void testConcurrentReservations() throws InterruptedException {
        int numberOfThreads = 20;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        for (long i = 1; i <= numberOfThreads; i++) {
            final long userId = i;
            executorService.submit(() -> {
                try {
                    // 예약 요청 시뮬레이션
                    ticketService.reserveTicket(ticketCategoryId, userId);
                } catch (Exception e) {
                    System.out.println("Exception: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // 예약된 티켓 수 확인
        long reservedTickets = ticketRepository.count();
        assertEquals(10, reservedTickets);

        // 남은 티켓 수 확인
        TicketCategory ticketCategory = ticketCategoryRepository.findById(ticketCategoryId).orElseThrow();
        assertEquals(0, ticketCategory.getRemainingQuantity());

        RAtomicLong stock = redissonClient.getAtomicLong("stock:ticketCategory:" + ticketCategoryId);
        assertEquals(0, stock.get());
    }
}