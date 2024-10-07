package com.neon.tamago.service;

import com.neon.tamago.model.Event;
import com.neon.tamago.model.EventType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EventServiceTest {

    @Autowired
    private EventService eventService;

    @Test
    void createEvent_success() {
        // Given
        Event event = new Event(
                "Spring Boot Workshop",
                "Learn Spring Boot",
                "http://example.com/image.png",
                "<p>Detail Content</p>",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(3),
                EventType.ONLINE,
                1L // creatorId
        );

        // When
        Event createdEvent = eventService.createEvent(event);

        // Then
        assertNotNull(createdEvent.getId());
        assertEquals("Spring Boot Workshop", createdEvent.getTitle());
    }

    @Test
    void getOngoingEvents_success() {
        // Given
        // 테스트용 이벤트 생성
        Event event = new Event(
                "Test Event",
                "Test Description",
                "http://example.com/image.png",
                "<p>Detail Content</p>",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(3),
                EventType.ONLINE,
                1L
        );
        eventService.createEvent(event);

        // When
        List<Event> events = eventService.getOngoingEvents();

        // Then
        assertNotNull(events);
        assertFalse(events.isEmpty());
    }

    @Test
    void getEventById_success() {
        // Given
        Event event = new Event(
                "Test Event",
                "Test Description",
                "http://example.com/image.png",
                "<p>Detail Content</p>",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(3),
                EventType.ONLINE,
                1L
        );
        Event createdEvent = eventService.createEvent(event);

        // When
        Event foundEvent = eventService.getEventById(createdEvent.getId());

        // Then
        assertEquals(createdEvent.getId(), foundEvent.getId());
    }
}