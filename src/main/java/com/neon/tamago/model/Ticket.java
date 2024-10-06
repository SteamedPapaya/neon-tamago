package com.neon.tamago.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;

    private String name;

    private String description;

    private Double price;

    private Integer quantity;

    private Long ownerId;

    private boolean reserved = false;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<EventTicket> eventTickets = new HashSet<>();

    public void addEvent(Event event) {
        EventTicket eventTicket = new EventTicket(event, this);
        eventTickets.add(eventTicket);
        event.getEventTickets().add(eventTicket);
    }

    public void removeEvent(Event event) {
        EventTicket eventTicket = new EventTicket(event, this);
        eventTickets.remove(eventTicket);
        event.getEventTickets().remove(eventTicket);
        eventTicket.setEvent(null);
        eventTicket.setTicket(null);
    }

    // 예약 메서드
    public void reserve(Long userId) {
        this.ownerId = userId;
        this.reserved = true;
    }
}
