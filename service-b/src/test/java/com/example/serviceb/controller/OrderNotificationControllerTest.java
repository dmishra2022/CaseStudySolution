package com.example.serviceb.controller;

import com.example.serviceb.service.OrderNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.UUID;

import static org.mockito.Mockito.*;

@WebMvcTest(OrderNotificationController.class)
public class OrderNotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderNotificationService orderNotificationService;

    @Test
    public void testOrderNotificationEndpoint() throws Exception {
        // Arrange
        UUID orderId = UUID.randomUUID();

        String orderNotificationJson = "{\"orderId\":\"" + orderId.toString() + "\"}";
        // Act
        mockMvc.perform(MockMvcRequestBuilders.post("/api/order-notification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderNotificationJson))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().string("Order notification received and processed"));

        // Assert
        verify(orderNotificationService, times(1)).handleOrderNotification(orderId);
    }
}