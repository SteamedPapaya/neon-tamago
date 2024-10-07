package com.neon.tamago.repository;

import com.neon.tamago.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findBySalesStartTimeBeforeAndSalesEndTimeAfter(LocalDateTime now1, LocalDateTime now2);
}