package com.example.serviceb.service;

import com.example.serviceb.entity.OrderNotification;
import com.example.serviceb.repository.OrderNotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.UUID;

import static com.example.serviceb.dto.OrderStatus.PENDING;
import static com.example.serviceb.dto.OrderStatus.PROCESSED;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class OrderNotificationServiceTest {

    @Mock
    private OrderNotificationRepository orderNotificationRepository;

    @InjectMocks
    private OrderNotificationService orderNotificationService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testHandleOrderNotification_NewOrder() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        when(orderNotificationRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        OrderNotification savedNotification = new OrderNotification();
        savedNotification.setOrderId(orderId);
        savedNotification.setStatus(PENDING);
        when(orderNotificationRepository.save(any(OrderNotification.class))).thenReturn(savedNotification);

        // Act
        orderNotificationService.handleOrderNotification(orderId);

        // Assert
        verify(orderNotificationRepository, times(1)).findByOrderId(orderId);
        verify(orderNotificationRepository, times(1)).save(any(OrderNotification.class));
    }

    @Test
    public void testHandleOrderNotification_DuplicateOrder() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        OrderNotification existingNotification = new OrderNotification();
        existingNotification.setOrderId(orderId);
        existingNotification.setStatus(PROCESSED);
        when(orderNotificationRepository.findByOrderId(orderId)).thenReturn(Optional.of(existingNotification));

        // Act
        orderNotificationService.handleOrderNotification(orderId);

        // Assert
        verify(orderNotificationRepository, times(1)).findByOrderId(orderId);
        verify(orderNotificationRepository, never()).save(any(OrderNotification.class));
    }

    @Test
    public void testHandleOrderNotification_OrderAlreadyExistsNotProcessed() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        OrderNotification existingNotification = new OrderNotification();
        existingNotification.setOrderId(orderId);
        existingNotification.setStatus(PENDING);
        when(orderNotificationRepository.findByOrderId(orderId)).thenReturn(Optional.of(existingNotification));
        OrderNotification savedNotification = new OrderNotification();
        savedNotification.setOrderId(orderId);
        savedNotification.setStatus(PROCESSED);
        when(orderNotificationRepository.save(any(OrderNotification.class))).thenReturn(savedNotification);

        // Act
        orderNotificationService.handleOrderNotification(orderId);

        // Assert
        verify(orderNotificationRepository, times(1)).findByOrderId(orderId);
        verify(orderNotificationRepository, times(1)).save(any(OrderNotification.class));
        assertTrue(existingNotification.getStatus() == PROCESSED);
    }
}