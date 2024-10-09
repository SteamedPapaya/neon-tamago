package com.neon.tamago.service;

import com.neon.tamago.exception.ResourceNotFoundException;
import com.neon.tamago.model.Ticket;
import com.neon.tamago.model.TicketCategory;
import com.neon.tamago.model.TicketStatus;
import com.neon.tamago.repository.TicketCategoryRepository;
import com.neon.tamago.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TicketServiceTest {

    @InjectMocks
    private TicketService ticketService;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketCategoryRepository ticketCategoryRepository;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RAtomicLong rAtomicLong;

    @BeforeEach
    public void setup() {
        // MockitoExtension이 Mock 객체를 초기화하므로 별도 작업 필요 없음
    }

    @Test
    public void testCancelTicket_Success() {
        // Given
        Long ticketId = 1L;
        Long ticketCategoryId = 100L;
        TicketCategory ticketCategory = new TicketCategory();
        ticketCategory.setId(ticketCategoryId);

        Ticket ticket = new Ticket(ticketCategory);
        ticket.setId(ticketId);
        ticket.reserve(1L);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(ticketCategoryRepository.increaseRemainingQuantity(ticketCategoryId)).thenReturn(1);
        when(redissonClient.getAtomicLong("stock:ticketCategory:" + ticketCategoryId)).thenReturn(rAtomicLong);

        // When
        ticketService.cancelTicket(ticketId);

        // Then
        assertEquals(TicketStatus.AVAILABLE, ticket.getStatus());
        assertNull(ticket.getUserId());
        assertNull(ticket.getReservedAt());
        assertNotNull(ticket.getCanceledAt());

        verify(ticketRepository).save(ticket);
        verify(ticketCategoryRepository).increaseRemainingQuantity(ticketCategoryId);
        verify(rAtomicLong).incrementAndGet();
    }

    @Test
    public void testCancelTicket_TicketNotFound() {
        // Given
        Long ticketId = 1L;

        // 불필요한 Stubbing 제거
        // when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            ticketService.cancelTicket(ticketId);
        });
    }
}