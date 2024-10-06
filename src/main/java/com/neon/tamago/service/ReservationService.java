package com.neon.tamago.service;

import com.neon.tamago.model.Event;
import com.neon.tamago.model.Ticket;
import com.neon.tamago.repository.TicketRepository;
import com.neon.tamago.exception.TicketNotAvailableException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

@Service
public class ReservationService {

    private final TicketRepository ticketRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public ReservationService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @Transactional
    public Ticket reserveTicket(Event event, Long userId) {
        // 비관적 락을 사용해 예약 가능한 티켓을 가져옴
        Ticket availableTicket = entityManager.createQuery("SELECT t FROM Ticket t WHERE t.eventTickets.event = :event AND t.reserved = false", Ticket.class)
                .setParameter("event", event)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .setMaxResults(1)
                .getSingleResult();

        if (availableTicket == null) {
            throw new TicketNotAvailableException("No available tickets for this event.");
        }

        // 티켓을 특정 사용자에게 예약
        availableTicket.reserve(userId);
        return ticketRepository.save(availableTicket);
    }
}