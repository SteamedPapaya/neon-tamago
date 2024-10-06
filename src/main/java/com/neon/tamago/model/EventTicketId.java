package com.neon.tamago.model;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class EventTicketId implements Serializable {

    private Long eventId;
    private Long ticketId;
}