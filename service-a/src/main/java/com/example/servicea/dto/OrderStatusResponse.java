package com.example.servicea.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class OrderStatusResponse {

    private UUID orderId;
    private String status;
}
