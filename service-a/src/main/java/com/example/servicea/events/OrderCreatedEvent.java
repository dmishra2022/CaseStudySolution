package com.example.servicea.events;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
@Setter
public class OrderCreatedEvent extends ApplicationEvent {
    private UUID orderId;

    public OrderCreatedEvent(UUID orderId) {
        super(orderId);
        this.orderId = orderId;
    }
}