package com.neon.tamago.repository;

import com.neon.tamago.model.TicketCategory;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TicketCategoryRepository extends JpaRepository<TicketCategory, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TicketCategory t WHERE t.id = :id")
    Optional<TicketCategory> findByIdForUpdate(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE TicketCategory t SET t.remainingQuantity = t.remainingQuantity - 1 WHERE t.id = :id AND t.remainingQuantity > 0")
    int decreaseRemainingQuantity(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE TicketCategory t SET t.remainingQuantity = t.remainingQuantity + 1 WHERE t.id = :id")
    int increaseRemainingQuantity(@Param("id") Long id);
}