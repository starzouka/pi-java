package org.example.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents the junction table between products and images.
 * Maps to `product_images` table with composite key (product_id, image_id)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductImage {
    private int productId;    // FK → products.product_id
    private int imageId;      // FK → images.image_id
    private int position;     // Position in product gallery (DEFAULT 1)

    // Convenience constructor
    public ProductImage(int productId, int imageId) {
        this.productId = productId;
        this.imageId = imageId;
        this.position = 1;
    }

    @Override
    public String toString() {
        return "ProductImage{" +
                "productId=" + productId +
                ", imageId=" + imageId +
                ", position=" + position +
                '}';
    }
}

