package com.example.servicea.dto;

import com.example.servicea.entity.Order;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class OrderRequest {
    @NotBlank
    private String productId;

    @Min(1)
    private int quantity;

    @Positive
    @NotBlank
    private double price;

    @NotBlank
    private String customerId;

    public Order toEntity() {
        Order order = new Order();
        order.setProduct(productId);
        order.setQuantity(quantity);
        order.setCustomerId(customerId);
        order.setPrice(price);
        return order;
    }
}
