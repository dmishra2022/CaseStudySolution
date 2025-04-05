package com.example.servicea.service;


import com.example.servicea.entity.Order;
import com.example.servicea.entity.NotificationOutbox;
import com.example.servicea.events.OrderCreatedEvent;
import com.example.servicea.repository.OrderRepository;
import com.example.servicea.repository.NotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor //  Use this instead of @Autowired fields
public class OrderService {

    private final OrderRepository orderRepository;
    private final NotificationOutboxRepository notificationOutboxRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Order createOrder(Order order) {
        Order savedOrder = orderRepository.save(order);

        // Create a NotificationOutbox entry
        NotificationOutbox outbox = new NotificationOutbox();
        outbox.setOrderId(savedOrder.getId());
        notificationOutboxRepository.save(outbox);

        // Publish an event.  A listener will process this asynchronously
        eventPublisher.publishEvent(new OrderCreatedEvent(savedOrder.getId()));

        return savedOrder;
    }

    public Optional<NotificationOutbox> findOrderStatus(UUID id) {
        return notificationOutboxRepository.findByOrderId(id);
    }
}