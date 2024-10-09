package com.neon.tamago.controller;

import com.neon.tamago.dto.TicketReservationRequest;
import com.neon.tamago.exception.UnauthorizedException;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
@Slf4j
public class TicketController {

    @Autowired
    private RabbitTemplate rabbitTemplate;

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

        // 예약 요청 DTO 생성
        TicketReservationRequest reservationRequest = new TicketReservationRequest(ticketCategoryId, userId);

        // 메시지 큐에 예약 요청 전송
        try {
            rabbitTemplate.convertAndSend("ticket-reservation-queue", reservationRequest);
        } catch (Exception e) {
            log.error("Failed to send message to the queue: {}", e.getMessage());
            // 예외 발생 시 즉시 응답을 반환하거나, 에러 응답을 반환하도록 처리
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to process reservation request");
        }

        // 즉시 성공 응답 반환
        return ResponseEntity.ok("Reservation request received");
    }

    private Long getUserIdFromRequest(HttpServletRequest request) throws UnauthorizedException {
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader == null) {
            throw new UnauthorizedException("User ID is missing in the request header");
        }
        return Long.parseLong(userIdHeader);
    }
}