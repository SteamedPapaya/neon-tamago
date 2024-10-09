package com.neon.tamago.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TicketTest {

    @Test
    public void testCancelReservation() {
        // Given
        TicketCategory ticketCategory = new TicketCategory();
        Ticket ticket = new Ticket(ticketCategory);
        Long userId = 1L;
        ticket.reserve(userId);

        // When
        ticket.cancelReservation();

        // Then
        assertEquals(TicketStatus.AVAILABLE, ticket.getStatus());
        assertNull(ticket.getUserId());
        assertNull(ticket.getReservedAt());
        assertNotNull(ticket.getCanceledAt());
    }
}