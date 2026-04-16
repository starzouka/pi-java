package org.example.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    private int orderId;
    private String orderNumber;
    private int cartId;
    private int userId;
    private String status; // PENDING | PAID | CANCELLED | SHIPPED | DELIVERED
    private String paymentMethod; // CARD | CASH | OTHER
    private String paymentStatus; // UNPAID | PAID | REFUNDED
    private double totalAmount;
    private String shippingAddress;
    private String phoneForDelivery;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;

    // Constructors for convenience
    public Order(int orderId, String orderNumber, int cartId, int userId, String status, double totalAmount) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.cartId = cartId;
        this.userId = userId;
        this.status = status;
        this.totalAmount = totalAmount;
        this.paymentStatus = "UNPAID";
    }

    public Order(String orderNumber, int cartId, int userId, double totalAmount) {
        this.orderNumber = orderNumber;
        this.cartId = cartId;
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.status = "PENDING";
        this.paymentStatus = "UNPAID";
    }
}

