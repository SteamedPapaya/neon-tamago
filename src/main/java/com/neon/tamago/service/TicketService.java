package com.neon.tamago.service;

import com.neon.tamago.exception.ResourceNotFoundException;
import com.neon.tamago.exception.SoldOutException;
import com.neon.tamago.model.Ticket;
import com.neon.tamago.model.TicketCategory;
import com.neon.tamago.repository.TicketCategoryRepository;
import com.neon.tamago.repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class TicketService {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TicketCategoryRepository ticketCategoryRepository;

    @Transactional
    public Ticket reserveTicket(Long ticketCategoryId, Long userId) throws SoldOutException {
        // 비관적 락 적용하여 티켓 카테고리 조회
        TicketCategory ticketCategory = ticketCategoryRepository.findByIdForUpdate(ticketCategoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket category not found"));

        if (ticketCategory.getRemainingQuantity() <= 0) {
            throw new SoldOutException("No tickets available");
        }

        // 남은 티켓 수 감소
        ticketCategory.setRemainingQuantity(ticketCategory.getRemainingQuantity() - 1);
        ticketCategoryRepository.save(ticketCategory);

        // 티켓 생성 및 예약
        Ticket ticket = new Ticket(ticketCategory);
        ticket.reserve(userId);
        return ticketRepository.save(ticket);
    }
}