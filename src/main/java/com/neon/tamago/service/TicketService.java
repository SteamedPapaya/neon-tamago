package com.neon.tamago.service;

import com.neon.tamago.dto.TicketReservationRequest;
import com.neon.tamago.exception.ResourceNotFoundException;
import com.neon.tamago.exception.SoldOutException;
import com.neon.tamago.model.Ticket;
import com.neon.tamago.model.TicketCategory;
import com.neon.tamago.repository.TicketCategoryRepository;
import com.neon.tamago.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RLock;
import org.redisson.api.RQueue;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketCategoryRepository ticketCategoryRepository;
    private final RedissonClient redissonClient;
    private final RabbitTemplate rabbitTemplate;
    private final AmqpTemplate amqpTemplate;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reserveTicket(Long ticketCategoryId, Long userId) throws Exception {
        // 분산 락 키 생성
        String lockKey = "lock:reservation:" + ticketCategoryId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean isLocked = false;
        try {
            // 락 시도
            isLocked = lock.tryLock(5, 10, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new Exception("Could not acquire lock");
            }

            // 남은 티켓 수 확인 및 원자적 감소
            String stockKey = "stock:ticketCategory:" + ticketCategoryId;
            RAtomicLong stock = redissonClient.getAtomicLong(stockKey);

            long updatedStock = stock.decrementAndGet();
            if (updatedStock < 0) {
                // 재고 부족, 감소 취소
                stock.incrementAndGet();
                throw new Exception("No tickets available");
            }

            // 데이터베이스에 티켓 예약 정보 저장
            TicketCategory ticketCategory = ticketCategoryRepository.findById(ticketCategoryId)
                    .orElseThrow(() -> new Exception("Ticket category not found"));

            Ticket ticket = new Ticket(ticketCategory);
            ticket.reserve(userId);
            ticketRepository.save(ticket);

            // 남은 티켓 수 감소 (데이터베이스)
            ticketCategoryRepository.decreaseRemainingQuantity(ticketCategoryId);

            System.out.println("User " + userId + " reserved ticket " + ticket.getId());

        } finally {
            if (isLocked) {
                lock.unlock();
            }
        }
    }

    public void processWaitingList(Long ticketCategoryId) {
        String queueKey = "queue:reservation:" + ticketCategoryId;
        RQueue<Long> queue = redissonClient.getQueue(queueKey);

        String stockKey = "stock:ticketCategory:" + ticketCategoryId;
        RAtomicLong stock = redissonClient.getAtomicLong(stockKey);

        while (stock.get() > 0 && !queue.isEmpty()) {
            Long userId = queue.poll();
            if (userId != null) {
                // 원자적으로 남은 수량 감소
                long updatedStock = stock.decrementAndGet();
                if (updatedStock >= 0) {
                    // 예약 요청 DTO 생성
                    TicketReservationRequest reservationRequest = new TicketReservationRequest(ticketCategoryId, userId);

                    // 메시지 큐에 예약 요청 전송
                    rabbitTemplate.convertAndSend("ticket-reservation-queue", reservationRequest);

                    // 사용자에게 알림을 보낼 수 있습니다 (예: 이메일, 푸시 알림 등)
                } else {
                    // 수량 감소 실패 시 다시 증가시키고 루프 종료
                    stock.incrementAndGet();
                    break;
                }
            }
        }
    }

    @Transactional
    public void cancelTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        ticket.cancelReservation();
        ticketRepository.save(ticket);

        // 남은 티켓 수 증가
        Long ticketCategoryId = ticket.getTicketCategory().getId();
        ticketCategoryRepository.increaseRemainingQuantity(ticketCategoryId);

        // Redis 남은 수량 증가
        String stockKey = "stock:ticketCategory:" + ticketCategoryId;
        RAtomicLong stock = redissonClient.getAtomicLong(stockKey);
        stock.incrementAndGet();

        // 대기열 처리
        processWaitingList(ticketCategoryId);
    }

    @Transactional(readOnly = true)
    public long getRemainingTickets(Long ticketCategoryId) {
        // 데이터베이스에서 티켓 카테고리에 따른 남은 티켓 수를 가져오는 로직 구현
        // 예시: 남은 티켓 수가 ticket_category 테이블의 특정 컬럼에 저장되어 있다고 가정
        return ticketRepository.findRemainingTicketsByCategoryId(ticketCategoryId);
    }
}