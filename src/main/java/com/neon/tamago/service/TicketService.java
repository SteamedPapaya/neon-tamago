package com.neon.tamago.service;

import com.neon.tamago.model.Ticket;
import com.neon.tamago.repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TicketService {

    @Autowired
    private TicketRepository ticketRepository;

    public List<Ticket> getAllTickets() {
        return ticketRepository.findAll();
    }

    public Optional<Ticket> getTicketById(Long id) {
        return ticketRepository.findById(id);
    }

    public Ticket saveTicket(Ticket ticket) {
        return ticketRepository.save(ticket);
    }

    public Optional<Ticket> updateTicket(Long id, Ticket ticketDetails) {
        return ticketRepository.findById(id).map(ticket -> {
            ticket.setName(ticketDetails.getName());
            ticket.setDescription(ticketDetails.getDescription());
            ticket.setPrice(ticketDetails.getPrice());
            ticket.setQuantity(ticketDetails.getQuantity());
            ticket.setEvent(ticketDetails.getEvent());
            return ticketRepository.save(ticket);
        });
    }

    public boolean deleteTicket(Long id) {
        return ticketRepository.findById(id).map(ticket -> {
            ticketRepository.delete(ticket);
            return true;
        }).orElse(false);
    }
}