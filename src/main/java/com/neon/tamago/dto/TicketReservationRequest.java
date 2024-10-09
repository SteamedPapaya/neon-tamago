package com.neon.tamago.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TicketReservationRequest {
    private Long ticketCategoryId;
    private Long userId;
}