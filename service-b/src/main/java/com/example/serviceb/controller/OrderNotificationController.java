package com.example.serviceb.controller;

import com.example.serviceb.dto.OrderNotificationRequest;
import com.example.serviceb.entity.OrderNotification;
import com.example.serviceb.service.OrderNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/order-notification")
public class OrderNotificationController {

    private final OrderNotificationService orderNotificationService;

    @PostMapping
    public ResponseEntity<String> orderNotification(@RequestBody OrderNotificationRequest orderNotificationRequest) {
        log.info("Received order notification for order ID: {}", orderNotificationRequest.getOrderId());
        orderNotificationService.handleOrderNotification(orderNotificationRequest.getOrderId());
        return ResponseEntity.ok("Order notification received and processed");
    }

    @GetMapping("{id}")
    public ResponseEntity<OrderNotification> findOrderStatus(@PathVariable UUID id) {
        return orderNotificationService.findOrderNotificationService(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
