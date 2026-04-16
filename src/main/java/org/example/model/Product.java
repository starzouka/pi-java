package org.example.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Product {
    private int productId;
    private int teamId;
    private String name;
    private String description;
    private double price;
    private int stockQty;
    private String sku;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String imageUrl; // ← NOUVEAU: URL de l'image principale

    // ...existing code...
    public Product(int productId, int teamId, String name, String description, double price, int stockQty, String sku, boolean isActive) {
        this.productId = productId;
        this.teamId = teamId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stockQty = stockQty;
        this.sku = sku;
        this.isActive = isActive;
    }

    public Product(int teamId, String name, String description, double price, int stockQty, String sku) {
        this.teamId = teamId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stockQty = stockQty;
        this.sku = sku;
        this.isActive = true;
    }
}

