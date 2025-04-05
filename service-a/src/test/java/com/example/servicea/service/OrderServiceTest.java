package com.example.servicea.service;

import static org.junit.jupiter.api.Assertions.*;

import com.example.servicea.entity.NotificationOutbox;
import com.example.servicea.entity.Order;
import com.example.servicea.events.OrderCreatedEvent;
import com.example.servicea.repository.NotificationOutboxRepository;
import com.example.servicea.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private NotificationOutboxRepository notificationOutboxRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @InjectMocks
    private OrderService orderService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testCreateOrder_Success() {
        // Arrange
        Order order = new Order();
        order.setCustomerId("TestCustomerID");
        order.setProduct("TestProductID");
        order.setQuantity(1);
        order.setPrice(1000);

        Order savedOrder = new Order();
        savedOrder.setId(UUID.randomUUID());
        savedOrder.setCustomerId(order.getCustomerId());
        savedOrder.setProduct(order.getProduct());
        savedOrder.setQuantity(order.getQuantity());
        savedOrder.setPrice(order.getPrice());

        when(orderRepository.save(order)).thenReturn(savedOrder);

        // Act
        Order result = orderService.createOrder(order);

        // Assert
        assertNotNull(result.getId());
        assertEquals(savedOrder.getCustomerId(), result.getCustomerId());
        assertEquals(savedOrder.getProduct(), result.getProduct());
        assertEquals(savedOrder.getQuantity(), result.getQuantity());

        // Verify that notificationOutboxRepository.save was called
        verify(notificationOutboxRepository, times(1)).save(any(NotificationOutbox.class));

        // Verify that the eventPublisher.publishEvent was called
        verify(eventPublisher, times(1)).publishEvent(any(OrderCreatedEvent.class));
    }

    @Test
    public void testFindOrderStatus_Found() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        NotificationOutbox outbox = new NotificationOutbox();
        outbox.setOrderId(orderId);

        when(notificationOutboxRepository.findByOrderId(orderId)).thenReturn(Optional.of(outbox));

        // Act
        Optional<NotificationOutbox> result = orderService.findOrderStatus(orderId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(orderId, result.get().getOrderId());
    }

    @Test
    public void testFindOrderStatus_NotFound() {
        // Arrange
        UUID orderId = UUID.randomUUID();

        when(notificationOutboxRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        // Act
        Optional<NotificationOutbox> result = orderService.findOrderStatus(orderId);

        // Assert
        assertFalse(result.isPresent());
    }
}