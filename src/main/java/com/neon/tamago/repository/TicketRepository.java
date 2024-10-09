package com.neon.tamago.repository;

import com.neon.tamago.model.Ticket;
import com.neon.tamago.model.Event;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // 티켓 카테고리별 남은 티켓 수 조회
    @Query("SELECT tc.remainingQuantity FROM TicketCategory tc WHERE tc.id = :ticketCategoryId")
    long findRemainingTicketsByCategoryId(Long ticketCategoryId);
}