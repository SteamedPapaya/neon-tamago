package com.neon.tamago.controller;

import com.neon.tamago.exception.UnauthorizedException;
import com.neon.tamago.model.TicketCategory;
import com.neon.tamago.service.TicketCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/events/{eventId}/ticket-categories")
public class TicketCategoryController {

    @Autowired
    private TicketCategoryService ticketCategoryService;

    @PostMapping
    public TicketCategory createTicketCategory(@PathVariable Long eventId, @RequestBody TicketCategory ticketCategory, HttpServletRequest request) throws UnauthorizedException {
        Long userId = getUserIdFromRequest(request);
        return ticketCategoryService.createTicketCategory(eventId, ticketCategory, userId);
    }

    private Long getUserIdFromRequest(HttpServletRequest request) {
        // JWT 토큰에서 userId 추출 로직 구현 (추후 구현 예정)
        return 1L; // 임시로 1L 반환
    }
}