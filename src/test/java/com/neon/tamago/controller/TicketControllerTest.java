package com.neon.tamago.controller;

import com.neon.tamago.dto.TicketReservationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisConnectionException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
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

        RLock lock = mock(RLock.class);
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        RAtomicLong stock = mock(RAtomicLong.class);
        when(redissonClient.getAtomicLong(anyString())).thenReturn(stock);
        when(stock.get()).thenReturn(10L);
        when(stock.decrementAndGet()).thenReturn(9L);

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

        RLock lock = mock(RLock.class);
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class)))
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

        RLock lock = mock(RLock.class);
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        // 락을 얻지 못하도록 설정
        when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);

        // When & Then
        mockMvc.perform(post("/api/tickets/reserve")
                        .param("ticketCategoryId", ticketCategoryId.toString())
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().string("Duplicate reservation request"));
    }
}