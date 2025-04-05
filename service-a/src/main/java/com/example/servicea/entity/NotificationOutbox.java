package com.example.servicea.entity;

import com.example.servicea.dto.OrderStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Entity
@Table(name = "notification_outbox")
@Data
public class NotificationOutbox {
    @Id
    @GeneratedValue
    private UUID id;
    private UUID orderId;
    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PENDING;
}
