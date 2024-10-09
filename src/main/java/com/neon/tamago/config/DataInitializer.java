package com.neon.tamago.config;

import com.neon.tamago.exception.UnauthorizedException;
import com.neon.tamago.model.*;
import com.neon.tamago.repository.EventRepository;
import com.neon.tamago.repository.TicketCategoryRepository;
import com.neon.tamago.service.EventService;
import com.neon.tamago.service.TicketCategoryService;
import com.neon.tamago.service.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private EventService eventService;

    @Autowired
    private TicketCategoryService ticketCategoryService;
    @Autowired
    private TicketService ticketService;

    @Override
    public void run(String... args) throws Exception {
        // 이벤트 생성
        Event event1 = new Event(
                "Spring Boot Workshop",
                "Learn Spring Boot with experts",
                "http://example.com/image1.png",
                "<p>Detailed content about Spring Boot Workshop</p>",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(7),
                LocalDateTime.now().plusDays(10),
                LocalDateTime.now().plusDays(10),
                EventType.ONLINE,
                1L // creatorId
        );
        event1 = eventService.createEvent(event1);

        Event event2 = new Event(
                "React Conference",
                "Join the React community",
                "http://example.com/image2.png",
                "<p>Detailed content about React Conference</p>",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(5),
                LocalDateTime.now().plusDays(15),
                LocalDateTime.now().plusDays(15),
                EventType.OFFLINE,
                2L // creatorId
        );
        event2 = eventService.createEvent(event2);

        // 티켓 카테고리 생성
        TicketCategory category1 = new TicketCategory(
                "Early Bird",
                "Discounted tickets for early registrants",
                BigDecimal.valueOf(50.00),
                100000000
        );
        try {
            ticketCategoryService.createTicketCategory(event1.getId(), category1, event1.getCreatorId());
        } catch (UnauthorizedException e) {
            throw new RuntimeException(e);
        }

        TicketCategory category2 = new TicketCategory(
                "Regular",
                "Standard tickets",
                BigDecimal.valueOf(100.00),
                200000
        );
        try {
            ticketCategoryService.createTicketCategory(event1.getId(), category2, event1.getCreatorId());
        } catch (UnauthorizedException e) {
            throw new RuntimeException(e);
        }

        TicketCategory category3 = new TicketCategory(
                "VIP",
                "VIP tickets with additional benefits",
                BigDecimal.valueOf(200.00),
                50000
        );
        try {
            ticketCategoryService.createTicketCategory(event1.getId(), category3, event1.getCreatorId());
        } catch (UnauthorizedException e) {
            throw new RuntimeException(e);
        }

        TicketCategory category4 = new TicketCategory(
                "Student",
                "Discounted tickets for students",
                BigDecimal.valueOf(30.00),
                150000
        );
        try {
            ticketCategoryService.createTicketCategory(event2.getId(), category4, event2.getCreatorId());
        } catch (UnauthorizedException e) {
            throw new RuntimeException(e);
        }

        TicketCategory category5 = new TicketCategory(
                "General Admission",
                "General admission tickets",
                BigDecimal.valueOf(80.00),
                300000
        );
        try {
            ticketCategoryService.createTicketCategory(event2.getId(), category5, event2.getCreatorId());
        } catch (UnauthorizedException e) {
            throw new RuntimeException(e);
        }

    }
}