package com.neon.tamago.controller;

import com.neon.tamago.dto.TicketReservationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisConnectionException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class TicketControllerTest {

    private MockMvc mockMvc;

    private TicketController ticketController;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private RedissonClient redissonClient;

    @BeforeEach
    public void setup() {
        // Mock 객체를 직접 주입하여 컨트롤러 생성
        ticketController = new TicketController(rabbitTemplate, redissonClient);
        mockMvc = MockMvcBuilders.standaloneSetup(ticketController).build();
    }

    @Test
    public void testReserveTicket_Success() throws Exception {
        // Given
        Long ticketCategoryId = 100L;
        Long userId = 1L;

        // 중복 요청 방지 기능 설정 (SET에 유저가 없다고 가정)
        RSet<Long> userSet = mock(RSet.class);
        when(redissonClient.<Long>getSet(anyString())).thenReturn(userSet);
        when(userSet.add(userId)).thenReturn(true); // 성공적으로 추가

        // Rate Limiter 설정
        RRateLimiter rateLimiter = mock(RRateLimiter.class);
        when(redissonClient.getRateLimiter(anyString())).thenReturn(rateLimiter);
        when(rateLimiter.trySetRate(any(), anyLong(), anyLong(), any())).thenReturn(true);
        when(rateLimiter.tryAcquire()).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/api/tickets/reserve")
                        .param("ticketCategoryId", ticketCategoryId.toString())
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("Reservation request received and is being processed."));

        verify(rabbitTemplate).convertAndSend(eq("ticket-reservation-queue"), any(TicketReservationRequest.class));
    }

    @Test
    public void testReserveTicket_DuplicateRequest() throws Exception {
        // Given
        Long ticketCategoryId = 100L;
        Long userId = 1L;

        // 중복 요청 방지 기능 설정 (SET에 이미 유저가 있다고 가정)
        RSet<Long> userSet = mock(RSet.class);
        when(redissonClient.<Long>getSet(anyString())).thenReturn(userSet);
        when(userSet.add(userId)).thenReturn(false); // 이미 존재하는 유저로 처리

        // Rate Limiter 설정
        RRateLimiter rateLimiter = mock(RRateLimiter.class);
        when(redissonClient.getRateLimiter(anyString())).thenReturn(rateLimiter);
        when(rateLimiter.trySetRate(any(), anyLong(), anyLong(), any())).thenReturn(true);
        when(rateLimiter.tryAcquire()).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/api/tickets/reserve")
                        .param("ticketCategoryId", ticketCategoryId.toString())
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("You have already made a reservation request for this ticket category."));
    }

    @Test
    public void testReserveTicket_RedisDown() throws Exception {
        // Given
        Long ticketCategoryId = 100L;
        Long userId = 1L;

        // Redis 연결이 실패하는 경우 시뮬레이션
        RSet<Long> userSet = mock(RSet.class);
        when(redissonClient.getSet(anyString())).thenThrow(new RedisConnectionException("Redis is down"));

        // Rate Limiter 설정
        RRateLimiter rateLimiter = mock(RRateLimiter.class);
        when(redissonClient.getRateLimiter(anyString())).thenReturn(rateLimiter);
        when(rateLimiter.trySetRate(any(), anyLong(), anyLong(), any())).thenReturn(true);
        when(rateLimiter.tryAcquire()).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/api/tickets/reserve")
                        .param("ticketCategoryId", ticketCategoryId.toString())
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())  // Expect 500 status code
                .andExpect(content().string("Reservation request failed due to Redis issue"));
    }
}