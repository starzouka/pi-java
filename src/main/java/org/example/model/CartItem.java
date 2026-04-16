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
public class CartItem {
    private int cartId;
    private int productId;
    private int quantity;
    private double unitPriceAtAdd;
    private LocalDateTime addedAt;
    private LocalDateTime updatedAt;
    private String productName;

    // Constructors for convenience
    public CartItem(int cartId, int productId, int quantity, double unitPriceAtAdd) {
        this.cartId = cartId;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPriceAtAdd = unitPriceAtAdd;
    }

    public CartItem(int cartId, int productId, int quantity, double unitPriceAtAdd, LocalDateTime addedAt) {
        this.cartId = cartId;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPriceAtAdd = unitPriceAtAdd;
        this.addedAt = addedAt;
    }

    public CartItem(int cartId, int productId, int quantity, double unitPriceAtAdd, String productName) {
        this.cartId = cartId;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPriceAtAdd = unitPriceAtAdd;
        this.productName = productName;
    }

    // Business method
    public double getTotalPrice() {
        return quantity * unitPriceAtAdd;
    }
}

