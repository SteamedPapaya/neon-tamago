package com.neon.tamago.service;

import com.neon.tamago.exception.ResourceNotFoundException;
import com.neon.tamago.exception.UnauthorizedException;
import com.neon.tamago.model.Event;
import com.neon.tamago.model.TicketCategory;
import com.neon.tamago.repository.TicketCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TicketCategoryService {

    @Autowired
    private TicketCategoryRepository ticketCategoryRepository;

    @Autowired
    private EventService eventService;

    @Transactional
    public TicketCategory createTicketCategory(Long eventId, TicketCategory ticketCategory, Long userId) throws UnauthorizedException {
        Event event = eventService.getEventById(eventId);
        if (!event.getCreatorId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to add ticket categories to this event");
        }
        ticketCategory.setEvent(event);
        return ticketCategoryRepository.save(ticketCategory);
    }

    // 필요 시 수정 및 삭제 메서드 추가

    public List<TicketCategory> findAll() {
        return ticketCategoryRepository.findAll();
    }
}