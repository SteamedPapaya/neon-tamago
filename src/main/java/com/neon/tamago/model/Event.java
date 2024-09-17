package com.neon.tamago.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
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

    @Enumerated(EnumType.STRING)
    private EventType type;  // 이벤트 유형 (워크숍, 콘서트, 모임 등)
}
