package com.neon.tamago.controller;

import com.neon.tamago.exception.SoldOutException;
import com.neon.tamago.exception.UnauthorizedException;
import com.neon.tamago.model.Ticket;
import com.neon.tamago.service.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    @Autowired
    private TicketService ticketService;

    @PostMapping("/reserve")
    public Ticket reserveTicket(@RequestParam Long ticketCategoryId, HttpServletRequest request) throws SoldOutException, UnauthorizedException {
        Long userId = getUserIdFromRequest(request);
        return ticketService.reserveTicket(ticketCategoryId, userId);
    }

    private Long getUserIdFromRequest(HttpServletRequest request) throws UnauthorizedException {
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader == null) {
            throw new UnauthorizedException("User ID is missing in the request header");
        }
        return Long.parseLong(userIdHeader);
    }
}