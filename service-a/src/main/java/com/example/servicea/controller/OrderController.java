package com.example.servicea.controller;

import com.example.servicea.dto.OrderRequest;
import com.example.servicea.dto.OrderStatusResponse;
import com.example.servicea.entity.Order;
import com.example.servicea.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @Validated
    public ResponseEntity<Order> createOrder(@Valid @RequestBody OrderRequest request) {
        if (request.getQuantity() <= 0 || request.getPrice() <= 0 || request.getProductId() == null || request.getProductId().isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }
        return ResponseEntity.ok(orderService.createOrder(request.toEntity()));
    }

    @GetMapping("/status/{id}")
    public ResponseEntity<OrderStatusResponse> findOrderStatus(@PathVariable UUID id) {
        return orderService.findOrderStatus(id)
                .map(data -> {
                    OrderStatusResponse response = new OrderStatusResponse();
                    response.setOrderId(data.getOrderId());
                    response.setStatus(data.getStatus().name());
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}