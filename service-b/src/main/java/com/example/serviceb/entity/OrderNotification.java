package com.example.serviceb.entity;

import com.example.serviceb.dto.OrderStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Entity
@Table(name = "order_notification")
@Data
public class OrderNotification {
    @Id
    private UUID orderId;
    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PENDING;
}