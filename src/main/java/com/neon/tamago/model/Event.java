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

    private String shortDescription;

    private String thumbnailImageUrl;

    @Lob
    private String detailContent;

    private LocalDateTime salesStartTime;

    private LocalDateTime salesEndTime;

    private LocalDateTime eventStartTime;

    private LocalDateTime eventEndTime;

    @Enumerated(EnumType.STRING)
    private EventType eventType;

    private Long creatorId; // 이벤트 생성자 ID (JWT에서 추출)

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TicketCategory> ticketCategories = new HashSet<>();

    // 생성자 및 연관 관계 메서드
    public Event(String title, String shortDescription, String thumbnailImageUrl, String detailContent,
                 LocalDateTime salesStartTime, LocalDateTime salesEndTime, LocalDateTime eventStartTime,
                 LocalDateTime eventEndTime, EventType eventType, Long creatorId) {
        this.title = title;
        this.shortDescription = shortDescription;
        this.thumbnailImageUrl = thumbnailImageUrl;
        this.detailContent = detailContent;
        this.salesStartTime = salesStartTime;
        this.salesEndTime = salesEndTime;
        this.eventStartTime = eventStartTime;
        this.eventEndTime = eventEndTime;
        this.eventType = eventType;
        this.creatorId = creatorId;
    }

    public void addTicketCategory(TicketCategory ticketCategory) {
        ticketCategories.add(ticketCategory);
        ticketCategory.setEvent(this);
    }

    public void removeTicketCategory(TicketCategory ticketCategory) {
        ticketCategories.remove(ticketCategory);
        ticketCategory.setEvent(null);
    }
}