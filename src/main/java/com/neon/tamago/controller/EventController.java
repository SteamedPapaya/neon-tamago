package com.neon.tamago.controller;

import com.neon.tamago.exception.UnauthorizedException;
import com.neon.tamago.model.Event;
import com.neon.tamago.service.EventService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    @Autowired
    private EventService eventService;

    @GetMapping
    public List<Event> getOngoingEvents() {
        return eventService.getOngoingEvents();
    }

    @GetMapping("/{eventId}")
    public Event getEvent(@PathVariable Long eventId) {
        return eventService.getEventById(eventId);
    }

    @PutMapping("/{eventId}")
    public Event updateEvent(@PathVariable Long eventId, @RequestBody Event eventDetails, HttpServletRequest request) throws UnauthorizedException {
        Long userId = getUserIdFromRequest(request);
        return eventService.updateEvent(eventId, eventDetails, userId);
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<?> deleteEvent(@PathVariable Long eventId, HttpServletRequest request) throws UnauthorizedException {
        Long userId = getUserIdFromRequest(request);
        eventService.deleteEvent(eventId, userId);
        return ResponseEntity.ok().build();
    }

    private Long getUserIdFromRequest(HttpServletRequest request) {
        // JWT 토큰에서 userId 추출 로직 구현 (추후 구현 예정)
        return 1L; // 임시로 1L 반환
    }
}