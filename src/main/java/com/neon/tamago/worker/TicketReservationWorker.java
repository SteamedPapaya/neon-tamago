package com.neon.tamago.worker;

import com.neon.tamago.dto.TicketReservationRequest;
import com.neon.tamago.exception.ResourceNotFoundException;
import com.neon.tamago.exception.SoldOutException;
import com.neon.tamago.model.Ticket;
import com.neon.tamago.model.TicketCategory;
import com.neon.tamago.repository.TicketCategoryRepository;
import com.neon.tamago.repository.TicketRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class TicketReservationWorker {

    private final TicketRepository ticketRepository;
    private final TicketCategoryRepository ticketCategoryRepository;

    public TicketReservationWorker(TicketRepository ticketRepository, TicketCategoryRepository ticketCategoryRepository) {
        this.ticketRepository = ticketRepository;
        this.ticketCategoryRepository = ticketCategoryRepository;
    }

    @RabbitListener(queues = "ticket-reservation-queue", containerFactory = "batchFactory")
    public void receiveMessages(List<TicketReservationRequest> requests) {
        log.info("Received batch of {} reservation requests", requests.size());
        for (TicketReservationRequest request : requests) {
            try {
                reserveTicket(request);
            } catch (Exception e) {
                log.error("Failed to reserve ticket for user {}: {}", request.getUserId(), e.getMessage());
                // 실패한 경우 재처리 로직 또는 DLQ(Dead Letter Queue)로 메시지 전송을 고려
            } catch (SoldOutException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Transactional
    public void reserveTicket(TicketReservationRequest request) throws SoldOutException {
        Long ticketCategoryId = request.getTicketCategoryId();
        Long userId = request.getUserId();

        // 티켓 카테고리 조회
        TicketCategory ticketCategory = ticketCategoryRepository.findById(ticketCategoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket category not found"));

        // 남은 티켓 수 검사 및 감소 (낙관적 업데이트)
        int updateCount = ticketCategoryRepository.decreaseRemainingQuantity(ticketCategoryId);
        if (updateCount == 0) {
            throw new SoldOutException("No tickets available");
        }

        // 티켓 생성 및 예약
        Ticket ticket = new Ticket(ticketCategory);
        ticket.reserve(userId);
        ticketRepository.save(ticket);

        log.info("User {} reserved ticket {}", userId, ticket.getId());
    }
}