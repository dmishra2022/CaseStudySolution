package com.example.servicea.repository;

import com.example.servicea.dto.OrderStatus;
import com.example.servicea.entity.NotificationOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, UUID> {

    @Query("SELECT n FROM NotificationOutbox n WHERE n.status = :status ORDER BY n.id ASC")
    List<NotificationOutbox> findUnprocessedNotifications(OrderStatus status);

    Optional<NotificationOutbox> findByOrderId(UUID orderId);
}