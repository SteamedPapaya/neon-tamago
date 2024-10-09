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
        rabbitTemplate.setMessageConverter(messageConverter);
    }

    @PostMapping("/reserve")
    public ResponseEntity<String> reserveTicket(@RequestParam Long ticketCategoryId, HttpServletRequest request) throws UnauthorizedException {
        Long userId = getUserIdFromRequest(request);

        try {
            // 사용자 Rate Limiter 설정 (Redis 기반)
            String rateLimiterKey = "rateLimiter:reservation:" + userId;
            RRateLimiter rateLimiter = redissonClient.getRateLimiter(rateLimiterKey);
            rateLimiter.trySetRate(RateType.OVERALL, 1L, 10L, RateIntervalUnit.SECONDS);

            // Rate Limiting 초과 시 처리
            if (!rateLimiter.tryAcquire()) {
                log.info("Rate limit exceeded for User {}", userId);
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("Too many requests, please try again later.");
            }

            // Redis 다운 여부를 먼저 체크 (여기서 RedisConnectionException 처리)
            String userSetKey = "ticket:reservation:users:" + ticketCategoryId;
            RSet<Long> userSet = redissonClient.getSet(userSetKey);

            // 사용자 요청 중복 여부 확인
            if (!userSet.add(userId)) {
                log.info("User {} already has a pending request for ticket category {}", userId, ticketCategoryId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("You have already made a reservation request for this ticket category.");
            }

            // 비동기 메시지 큐로 예약 요청 전송
            TicketReservationRequest reservationRequest = new TicketReservationRequest(ticketCategoryId, userId);
            rabbitTemplate.convertAndSend("ticket-reservation-queue", reservationRequest);

            return ResponseEntity.ok("Reservation request received and is being processed.");
        } catch (RedisConnectionException e) {
            log.error("Redis is down", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Reservation request failed due to Redis issue");
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