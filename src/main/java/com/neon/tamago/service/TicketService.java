package com.neon.tamago.service;

import com.neon.tamago.exception.ResourceNotFoundException;
import com.neon.tamago.exception.SoldOutException;
import com.neon.tamago.model.Ticket;
import com.neon.tamago.model.TicketCategory;
import com.neon.tamago.repository.TicketCategoryRepository;
import com.neon.tamago.repository.TicketRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
public class TicketService {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TicketCategoryRepository ticketCategoryRepository;

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Transactional(isolation = Isolation.SERIALIZABLE)
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
        Ticket savedTicket = ticketRepository.save(ticket);
        log.info("User {} reserved ticket {}", userId, ticket.getId());

        // todo: 메시지 전송
        // amqpTemplate.convertAndSend("ticket-reservation-queue", "Ticket reserved: " + savedTicket.getId());

        return savedTicket;
    }
}