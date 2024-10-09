package com.neon.tamago.controller;

import com.neon.tamago.dto.TicketReservationRequest;
import com.neon.tamago.exception.UnauthorizedException;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RLock;
import org.redisson.api.RQueue;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisConnectionException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@Slf4j
public class TicketController {

    private final RabbitTemplate rabbitTemplate;
    private final RedissonClient redissonClient;

    @Autowired
    private Jackson2JsonMessageConverter messageConverter;

    @PostConstruct
    public void init() {
        // 메시지 컨버터 설정
        rabbitTemplate.setMessageConverter(messageConverter);
    }

    @PostMapping("/reserve")
    public ResponseEntity<String> reserveTicket(@RequestParam Long ticketCategoryId, HttpServletRequest request) throws UnauthorizedException {
        Long userId = getUserIdFromRequest(request);

        // 분산 락 키 생성
        String lockKey = "lock:reservation:" + userId + ":" + ticketCategoryId;

        RLock lock = redissonClient.getLock(lockKey);
        boolean isLocked = false;
        try {
            isLocked = lock.tryLock(0, 10, TimeUnit.SECONDS);
            if (!isLocked) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("Duplicate reservation request");
            }

            // 남은 티켓 수 확인 및 원자적 감소
            String stockKey = "stock:ticketCategory:" + ticketCategoryId;
            RAtomicLong stock = redissonClient.getAtomicLong(stockKey);

            long currentStock = stock.get();
            if (currentStock <= 0) {
                // 남은 티켓이 없으므로 대기열에 추가
                String queueKey = "queue:reservation:" + ticketCategoryId;
                RQueue<Long> queue = redissonClient.getQueue(queueKey);
                queue.add(userId);

                return ResponseEntity.ok("All tickets are sold out. You have been added to the waiting list.");
            }

            // 원자적으로 남은 수량 감소
            long updatedStock = stock.decrementAndGet();
            if (updatedStock < 0) {
                // 재고 부족, 감소 취소 및 대기열에 추가
                stock.incrementAndGet();

                String queueKey = "queue:reservation:" + ticketCategoryId;
                RQueue<Long> queue = redissonClient.getQueue(queueKey);
                queue.add(userId);

                return ResponseEntity.ok("All tickets are sold out. You have been added to the waiting list.");
            }

            // 예약 요청 DTO 생성
            TicketReservationRequest reservationRequest = new TicketReservationRequest(ticketCategoryId, userId);

            // 메시지 큐에 예약 요청 전송
            rabbitTemplate.convertAndSend("ticket-reservation-queue", reservationRequest);

            return ResponseEntity.ok("Reservation request received");
        } catch (RedisConnectionException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Reservation request failed");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Reservation request failed");
        } finally {
            if (isLocked) {
                lock.unlock();
            }
        }
    }

    private Long getUserIdFromRequest(HttpServletRequest request) throws UnauthorizedException {
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader == null) {
            throw new UnauthorizedException("User ID is missing in the request header");
        }
        return Long.parseLong(userIdHeader);
    }
}