package com.neon.tamago.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;

    @Enumerated(EnumType.STRING)
    private TicketStatus status = TicketStatus.AVAILABLE;

    private Long userId; // 예매자 ID

    private LocalDateTime reservedAt;

    private LocalDateTime usedAt;

    private LocalDateTime canceledAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_category_id", nullable = false)
    private TicketCategory ticketCategory;

    // 생성자 및 메서드
    public Ticket(TicketCategory ticketCategory) {
        this.ticketCategory = ticketCategory;
        this.code = UUID.randomUUID().toString();
    }

    public void reserve(Long userId) {
        this.userId = userId;
        this.reservedAt = LocalDateTime.now();
        this.status = TicketStatus.RESERVED;
    }

    public void use() {
        this.usedAt = LocalDateTime.now();
        this.status = TicketStatus.USED;
    }

    public void cancelReservation() {
        this.userId = null;
        this.reservedAt = null;
        this.canceledAt = LocalDateTime.now();
        this.status = TicketStatus.AVAILABLE;
    }
}