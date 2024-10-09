package com.neon.tamago.controller;

import com.neon.tamago.dto.TicketReservationRequest;
import com.neon.tamago.service.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.*;
import org.redisson.client.RedisConnectionException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class TicketControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RRateLimiter rateLimiter;

    @Mock
    private RLock lock;

    @Mock
    private RAtomicLong stock;

    @Mock
    private RScript rScript;

    @Mock
    private TicketService ticketService; // Assuming there's a TicketService

    @InjectMocks
    private TicketController ticketController; // The controller under test

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(ticketController).build();
    }

    @Test
    public void testReserveTicket_Success() throws Exception {
        // Given
        Long ticketCategoryId = 100L;
        Long userId = 1L;

        // Mock rateLimiter
        when(redissonClient.getRateLimiter("rateLimiter:reservation:" + userId)).thenReturn(rateLimiter);
        when(rateLimiter.trySetRate(RateType.OVERALL, 1L, 10L, RateIntervalUnit.SECONDS)).thenReturn(true);
        when(rateLimiter.tryAcquire()).thenReturn(true);

        // Mock lock
        when(redissonClient.getLock("lock:reservation:" + ticketCategoryId)).thenReturn(lock);
        when(lock.tryLock(0L, 10L, TimeUnit.SECONDS)).thenReturn(true);

        // Mock stock
        when(redissonClient.getAtomicLong("stock:ticketCategory:" + ticketCategoryId)).thenReturn(stock);
        when(stock.get()).thenReturn(10L);
//        when(stock.decrementAndGet()).thenReturn(9L);

        // Mock RScript
        when(redissonClient.getScript()).thenReturn(rScript);
        String luaScript = "if (redis.call('GET', KEYS[1]) == false) then return -1; end; local stock = tonumber(redis.call('GET', KEYS[1])); if (stock <= 0) then return -1; else redis.call('DECR', KEYS[1]); return stock - 1; end;";
        List<String> keys = Collections.singletonList("stock:ticketCategory:" + ticketCategoryId);
        when(rScript.eval(
                eq(RScript.Mode.READ_WRITE),
                anyString(), // 유연한 매칭
                eq(RScript.ReturnType.INTEGER),
                anyList(),    // 유연한 매칭
                any(Object[].class) // 가변 인자 매칭
        )).thenReturn(9L);

        // Mock TicketService (if used)
//        when(ticketService.getRemainingTickets(ticketCategoryId)).thenReturn(9L);

        // When & Then
        mockMvc.perform(post("/api/tickets/reserve")
                        .param("ticketCategoryId", ticketCategoryId.toString())
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("Reservation request received"));

        verify(rabbitTemplate).convertAndSend(eq("ticket-reservation-queue"), any(TicketReservationRequest.class));
    }

    @Test
    public void testReserveTicket_RedisDown() throws Exception {
        // Given
        Long ticketCategoryId = 100L;
        Long userId = 1L;

        // Mock rateLimiter
        when(redissonClient.getRateLimiter("rateLimiter:reservation:" + userId)).thenReturn(rateLimiter);
        when(rateLimiter.trySetRate(RateType.OVERALL, 1L, 10L, RateIntervalUnit.SECONDS)).thenReturn(true);
        when(rateLimiter.tryAcquire()).thenReturn(true);

        // Mock lock to throw exception
        when(redissonClient.getLock("lock:reservation:" + ticketCategoryId)).thenReturn(lock);
        when(lock.tryLock(0L, 10L, TimeUnit.SECONDS))
                .thenThrow(new RedisConnectionException("Redis is down"));

        // When & Then
        mockMvc.perform(post("/api/tickets/reserve")
                        .param("ticketCategoryId", ticketCategoryId.toString())
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Reservation request failed"));
    }

    @Test
    public void testReserveTicket_DuplicateRequest() throws Exception {
        // Given
        Long ticketCategoryId = 100L;
        Long userId = 1L;

        // Mock rateLimiter
        when(redissonClient.getRateLimiter("rateLimiter:reservation:" + userId)).thenReturn(rateLimiter);
        when(rateLimiter.trySetRate(RateType.OVERALL, 1L, 10L, RateIntervalUnit.SECONDS)).thenReturn(true);
        when(rateLimiter.tryAcquire()).thenReturn(true);

        // Mock lock to not acquire
        when(redissonClient.getLock("lock:reservation:" + ticketCategoryId)).thenReturn(lock);
        when(lock.tryLock(0L, 10L, TimeUnit.SECONDS)).thenReturn(false);

        // When & Then
        mockMvc.perform(post("/api/tickets/reserve")
                        .param("ticketCategoryId", ticketCategoryId.toString())
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().string("Duplicate reservation request"));
    }

    @Test
    public void testReserveTicket_RateLimiterExceeded() throws Exception {
        // Given
        Long ticketCategoryId = 100L;
        Long userId = 1L;

        // Mock rateLimiter to reject the request
        when(redissonClient.getRateLimiter("rateLimiter:reservation:" + userId)).thenReturn(rateLimiter);
        when(rateLimiter.trySetRate(RateType.OVERALL, 1L, 10L, RateIntervalUnit.SECONDS)).thenReturn(true);
        when(rateLimiter.tryAcquire()).thenReturn(false); // Reject due to rate limiting

        // When & Then
        mockMvc.perform(post("/api/tickets/reserve")
                        .param("ticketCategoryId", ticketCategoryId.toString())
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().string("Too many requests, please try again later."));
    }
}