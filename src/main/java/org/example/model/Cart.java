package org.example.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class Cart {
    private int cartId;
    private int userId;
    private String status; // OPEN | LOCKED | ORDERED
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lockedAt; // ✓ Synchronized with DB
    private List<CartItem> items;

    // Constructors
    public Cart() {
        this.items = new ArrayList<>();
        this.status = "OPEN";
    }

    public Cart(int cartId, int userId, String status) {
        this.cartId = cartId;
        this.userId = userId;
        this.status = status;
        this.items = new ArrayList<>();
    }

    public Cart(int cartId, int userId, String status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.cartId = cartId;
        this.userId = userId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.items = new ArrayList<>();
    }

    public Cart(int userId, String status, List<CartItem> items) {
        this.userId = userId;
        this.status = status;
        this.items = items != null ? items : new ArrayList<>();
    }





    public void addItem(CartItem item) {
        this.items.add(item);
    }

    public void removeItem(CartItem item) {
        this.items.remove(item);
    }

    public double getTotalPrice() {
        return items.stream().mapToDouble(CartItem::getTotalPrice).sum();
    }

    public int getTotalQuantity() {
        return items.stream().mapToInt(CartItem::getQuantity).sum();
    }

    public void clear() {
        this.items.clear();
    }

    @Override
    public String toString() {
        return "Cart{" +
                "cartId=" + cartId +
                ", userId=" + userId +
                ", status='" + status + '\'' +
                ", items=" + items.size() +
                ", totalPrice=" + getTotalPrice() +
                '}';
    }
}

