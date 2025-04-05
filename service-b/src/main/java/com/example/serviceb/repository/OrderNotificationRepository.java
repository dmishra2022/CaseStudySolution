package com.example.serviceb.repository;

import com.example.serviceb.entity.OrderNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderNotificationRepository extends JpaRepository<OrderNotification, UUID> {
    Optional<OrderNotification> findByOrderId(UUID orderId);
}