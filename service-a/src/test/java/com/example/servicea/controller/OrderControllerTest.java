package com.example.servicea.controller;

import com.example.servicea.dto.OrderRequest;
import com.example.servicea.dto.OrderStatus;
import com.example.servicea.entity.NotificationOutbox;
import com.example.servicea.entity.Order;
import com.example.servicea.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    private MockMvc mockMvc;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(orderController).build();
    }

    @Test
    public void testCreateOrder_Success() throws Exception {
        OrderRequest request = new OrderRequest();
        request.setCustomerId("Test Customer");
        request.setProductId("Test Product");
        request.setQuantity(1);
        request.setPrice(100);

        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setCustomerId("Test Customer");
        order.setProduct("Test Product");
        order.setQuantity(1);
        order.setPrice(100);

        when(orderService.createOrder(any(Order.class))).thenReturn(order);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"Test Customer\",\"productId\":\"Test Product\",\"quantity\":1,\"price\":100}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value("Test Customer"))
                .andExpect(jsonPath("$.product").value("Test Product"))
                .andExpect(jsonPath("$.quantity").value(1))
                .andExpect(jsonPath("$.price").value(100));
    }

    @Test
    public void testCreateOrder_InvalidInput() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"Test Customer\",\"productId\":\"Test Product\"}")) // Missing quantity and price
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testFindOrderStatus_Success() throws Exception {
        UUID orderId = UUID.randomUUID();
        NotificationOutbox response = new NotificationOutbox();
        response.setOrderId(orderId);
        response.setStatus(OrderStatus.PENDING);

        when(orderService.findOrderStatus(orderId)).thenReturn(Optional.of(response));

        mockMvc.perform(get("/api/orders/status/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    public void testFindOrderStatus_NotFound() throws Exception {
        UUID orderId = UUID.randomUUID();

        when(orderService.findOrderStatus(orderId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/orders/status/" + orderId))
                .andExpect(status().isNotFound());
    }
}