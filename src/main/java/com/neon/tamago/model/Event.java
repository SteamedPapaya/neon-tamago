package com.neon.tamago.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String description;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String location;

    private String coverImageUrl;

    private int likes;  // 좋아요 수

    private int ticketsLeft;  // 남은 티켓 수

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<EventTicket> eventTickets = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private EventType type;  // 이벤트 유형 (워크숍, 콘서트, 모임 등)

    public void addTicket(Ticket ticket) {
        EventTicket eventTicket = new EventTicket(this, ticket);
        eventTickets.add(eventTicket);
        ticket.getEventTickets().add(eventTicket);
    }

    public void removeTicket(Ticket ticket) {
        EventTicket eventTicket = new EventTicket(this, ticket);
        eventTickets.remove(eventTicket);
        ticket.getEventTickets().remove(eventTicket);
        eventTicket.setEvent(null);
        eventTicket.setTicket(null);
    }
}
