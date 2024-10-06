package com.neon.tamago.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class EventTicket {

    @EmbeddedId
    private EventTicketId id = new EventTicketId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("eventId")
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("ticketId")
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    public EventTicket(Event event, Ticket ticket) {
        this.event = event;
        this.ticket = ticket;
        this.id = new EventTicketId(event.getId(), ticket.getId());
    }
}