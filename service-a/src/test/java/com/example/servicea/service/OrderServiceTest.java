package com.example.servicea.service;

import static org.junit.jupiter.api.Assertions.*;

import com.example.servicea.dto.OrderStatus;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpServerErrorException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;


class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private NotificationOutboxRepository notificationOutboxRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private RestTemplate restTemplate;
    @InjectMocks
    private OrderService orderService;
    @InjectMocks
    private MessageRelay messageRelay;

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

    // MessageRelay Unit Tests
    @Test
    public void testProcessOutbox_Success() {
        // Arrange
        NotificationOutbox outbox1 = new NotificationOutbox();
        outbox1.setId(UUID.randomUUID());
        outbox1.setOrderId(UUID.randomUUID());
        outbox1.setStatus(OrderStatus.PENDING);

        NotificationOutbox outbox2 = new NotificationOutbox();
        outbox2.setId(UUID.randomUUID());
        outbox2.setOrderId(UUID.randomUUID());
        outbox2.setStatus(OrderStatus.PENDING);

        List<NotificationOutbox> outboxEntries = new ArrayList<>();
        outboxEntries.add(outbox1);
        outboxEntries.add(outbox2);

        when(notificationOutboxRepository.findUnprocessedNotifications(OrderStatus.PENDING)).thenReturn(outboxEntries);
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("Notification Sent"));

        // Act
        messageRelay.processOutbox();

        // Assert
        verify(restTemplate, times(2)).postForEntity(anyString(), any(), eq(String.class));
        verify(notificationOutboxRepository, times(2)).save(any(NotificationOutbox.class));

        // Verify that the outbox entries are marked as processed
        assertTrue(outbox1.getStatus() == OrderStatus.PROCESSED);
        assertTrue(outbox2.getStatus() == OrderStatus.PROCESSED);
    }

    @Test
    public void testProcessOutbox_ServiceBUnreachable() {
        // Arrange
        NotificationOutbox outbox = new NotificationOutbox();
        outbox.setId(UUID.randomUUID());
        outbox.setOrderId(UUID.randomUUID());
        outbox.setStatus(OrderStatus.PENDING);

        List<NotificationOutbox> outboxEntries = new ArrayList<>();
        outboxEntries.add(outbox);

        when(notificationOutboxRepository.findUnprocessedNotifications(OrderStatus.PENDING)).thenReturn(outboxEntries);
        HttpServerErrorException serverError = new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class))).thenThrow(serverError);

        // Act
        try {
            messageRelay.processOutbox();
        } catch (HttpServerErrorException e) {
            // Expected exception
        }

        // Assert
//        verify(restTemplate, times(5)).postForEntity(anyString(), any(), eq(String.class)); // Check retry count
        verify(notificationOutboxRepository, never()).save(any(NotificationOutbox.class)); // Should not update if sending fails
        assertFalse(outbox.getStatus() == OrderStatus.PROCESSED); // Status should remain PENDING
    }
}