package com.neon.tamago.controller;

import com.neon.tamago.exception.SoldOutException;
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
    public Ticket reserveTicket(@RequestParam Long ticketCategoryId, HttpServletRequest request) throws SoldOutException {
        Long userId = getUserIdFromRequest(request);
        return ticketService.reserveTicket(ticketCategoryId, userId);
    }

    private Long getUserIdFromRequest(HttpServletRequest request) {
        // JWT 토큰에서 userId 추출 로직 구현 (추후 구현 예정)
        return 1L; // 임시로 1L 반환
    }
}