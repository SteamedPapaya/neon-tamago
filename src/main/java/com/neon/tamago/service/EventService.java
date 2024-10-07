package com.neon.tamago.service;

import com.neon.tamago.exception.ResourceNotFoundException;
import com.neon.tamago.exception.UnauthorizedException;
import com.neon.tamago.model.Event;
import com.neon.tamago.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EventService {

    @Autowired
    private EventRepository eventRepository;

    @Transactional
    public Event createEvent(Event event) {
        // 추가 검증 로직이 있다면 구현
        return eventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<Event> getOngoingEvents() {
        LocalDateTime now = LocalDateTime.now();
        return eventRepository.findBySalesStartTimeBeforeAndSalesEndTimeAfter(now, now);
    }

    @Transactional(readOnly = true)
    public Event getEventById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id " + eventId));
    }

    @Transactional
    public Event updateEvent(Long eventId, Event eventDetails, Long userId) throws UnauthorizedException {
        Event event = getEventById(eventId);
        if (!event.getCreatorId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to update this event");
        }
        // 이벤트 필드 업데이트
        event.setTitle(eventDetails.getTitle());
        event.setShortDescription(eventDetails.getShortDescription());
        event.setThumbnailImageUrl(eventDetails.getThumbnailImageUrl());
        event.setDetailContent(eventDetails.getDetailContent());
        event.setSalesStartTime(eventDetails.getSalesStartTime());
        event.setSalesEndTime(eventDetails.getSalesEndTime());
        event.setEventStartTime(eventDetails.getEventStartTime());
        event.setEventEndTime(eventDetails.getEventEndTime());
        event.setEventType(eventDetails.getEventType());
        return eventRepository.save(event);
    }

    @Transactional
    public void deleteEvent(Long eventId, Long userId) throws UnauthorizedException {
        Event event = getEventById(eventId);
        if (!event.getCreatorId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to delete this event");
        }
        eventRepository.delete(event);
    }
}