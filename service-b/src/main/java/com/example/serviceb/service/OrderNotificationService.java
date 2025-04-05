package com.example.serviceb.service;

import com.example.serviceb.dto.OrderStatus;
import com.example.serviceb.entity.OrderNotification;
import com.example.serviceb.repository.OrderNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderNotificationService {

    private final OrderNotificationRepository orderNotificationRepository;

    @Transactional
    public void handleOrderNotification(UUID orderId) {
        log.info("Received order notification for order ID: {}", orderId);
        // Idempotency check:
        Optional<OrderNotification> existingNotification = orderNotificationRepository.findByOrderId(orderId);
        if (existingNotification.isPresent()) {
            if (existingNotification.get().getStatus() == OrderStatus.PROCESSED) {
                // Already processed, do nothing.  This is crucial for idempotency!
                return;
            } else {
                log.info("Updating existing order notification for order ID: {}", orderId);
                //update the existing notification
                OrderNotification orderNotification = existingNotification.get();
                orderNotification.setStatus(OrderStatus.PROCESSED);
                orderNotificationRepository.save(orderNotification);
                processOrder(orderId);
                return;
            }
        }

        // Process the order notification (e.g., update order status, send email, etc.)
        OrderNotification orderNotification = new OrderNotification();
        orderNotification.setOrderId(orderId);
        orderNotification.setStatus(OrderStatus.PROCESSED);
        orderNotificationRepository.save(orderNotification);
        processOrder(orderId);
    }

    private void processOrder(UUID orderId) {
        //  Actual business logic to handle the order notification.
        //  This method should be idempotent as well.
        System.out.println("Order notification received and processed for order ID: " + orderId);
        //  Add your business logic here.
    }

    public Optional<OrderNotification> findOrderNotificationService(UUID id) {
        log.info("Finding order notification for order ID: {}", id);
        return orderNotificationRepository.findByOrderId(id);
    }
}