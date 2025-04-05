package com.example.servicea.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Entity
@Table(name = "orders")
@Data
public class Order {
    @Id
    @GeneratedValue
    private UUID id;
    private String customerId;
    private String product;
    private int quantity;
    private double price;
}