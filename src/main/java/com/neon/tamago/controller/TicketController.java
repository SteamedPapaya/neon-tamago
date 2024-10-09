package com.neon.tamago.controller;

import com.neon.tamago.dto.TicketReservationRequest;
import com.neon.tamago.exception.UnauthorizedException;
import com.neon.tamago.service.TicketService;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.*;
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
    private TicketService ticketService;

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

        // 사용자 Rate Limiter 설정 (Redis 기반)
        String rateLimiterKey = "rateLimiter:reservation:" + userId;
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(rateLimiterKey);
        rateLimiter.trySetRate(RateType.OVERALL, 1L, 10L, RateIntervalUnit.SECONDS);  // 10초에 1번 요청 허용

        // Rate Limiting 초과 시 처리
        if (!rateLimiter.tryAcquire()) {
            log.info("Rate limit exceeded for User {}", userId);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("Too many requests, please try again later.");
        }

        // 분산 락 키 생성 (티켓 카테고리별로 처리)
        String lockKey = "lock:reservation:" + ticketCategoryId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean isLocked = false;

        try {
            isLocked = lock.tryLock(0, 10, TimeUnit.SECONDS);
            if (!isLocked) {
                log.info("Duplicate reservation request for User {}", userId);
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("Duplicate reservation request");
            }

            // 남은 티켓 수 캐시에서 가져오기
            String stockKey = "stock:ticketCategory:" + ticketCategoryId;
            long currentStock = getRemainingTickets(stockKey, ticketCategoryId);

            if (currentStock <= 0) {
                // 남은 티켓이 없으면 대기열에 추가
                addToWaitList(ticketCategoryId, userId);
                return ResponseEntity.ok("All tickets are sold out. You have been added to the waiting list.");
            }

            // 100개 이하 남은 티켓일 경우만 락 적용하여 처리 (병목 방지)
            if (currentStock <= 100) {
                long updatedStock = decreaseStockWithLuaScript(stockKey);

                if (updatedStock < 0) {
                    addToWaitList(ticketCategoryId, userId);
                    return ResponseEntity.ok("All tickets are sold out. You have been added to the waiting list.");
                }
            } else {
                // 100개 초과일 때는 캐시만 업데이트하고 바로 메시지 큐로 전송
                updateTicketStock(stockKey);
            }

            // 예약 요청 DTO 생성 및 메시지 큐 전송
            TicketReservationRequest reservationRequest = new TicketReservationRequest(ticketCategoryId, userId);
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

    private long getRemainingTickets(String stockKey, Long ticketCategoryId) {
        RAtomicLong cachedStock = redissonClient.getAtomicLong(stockKey);

        // 캐시에서 남은 티켓 수 확인
        if (cachedStock.get() > 0) {
            return cachedStock.get();
        }

        // 캐시가 만료된 경우 DB에서 새로 조회하고 캐시에 저장
        long stockFromDB = ticketService.getRemainingTickets(ticketCategoryId);
        cachedStock.set(stockFromDB);
        cachedStock.expire(60, TimeUnit.SECONDS); // 캐시 TTL 설정

        return stockFromDB;
    }

    private void addToWaitList(Long ticketCategoryId, Long userId) {
        String queueKey = "queue:reservation:" + ticketCategoryId;
        RQueue<Long> queue = redissonClient.getQueue(queueKey);
        queue.add(userId);
    }

    private void updateTicketStock(String stockKey) {
        RAtomicLong cachedStock = redissonClient.getAtomicLong(stockKey);
        cachedStock.decrementAndGet();
    }

    private long decreaseStockWithLuaScript(String stockKey) {
        String luaScript = "if (redis.call('GET', KEYS[1]) == false) then " +
                "  return -1;" +
                "end; " +
                "local stock = tonumber(redis.call('GET', KEYS[1])); " +
                "if (stock <= 0) then " +
                "  return -1; " +
                "else " +
                "  redis.call('DECR', KEYS[1]); " +
                "  return stock - 1; " +
                "end;";
        return (Long) redissonClient.getScript().eval(RScript.Mode.READ_WRITE, luaScript, RScript.ReturnType.INTEGER, java.util.Collections.singletonList(stockKey));
    }

    private Long getUserIdFromRequest(HttpServletRequest request) throws UnauthorizedException {
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader == null) {
            throw new UnauthorizedException("User ID is missing in the request header");
        }
        return Long.parseLong(userIdHeader);
    }
}