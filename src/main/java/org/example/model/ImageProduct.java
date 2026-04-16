package org.example.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Represents a product image.
 * Maps to the `images` table in the database.
 * Relationship handled through `product_images` junction table
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ImageProduct {
    private int imageId;           // image_id PK
    private String fileUrl;        // file_url VARCHAR(500)
    private String mimeType;       // mime_type VARCHAR(60)
    private Long sizeBytes;        // size_bytes BIGINT UNSIGNED
    private Integer width;         // width INT UNSIGNED
    private Integer height;        // height INT UNSIGNED
    private String altText;        // alt_text VARCHAR(255)
    private LocalDateTime createdAt;
    private Integer uploadedByUserId; // uploaded_by_user_id FK → users

    // Convenience constructor for basic image info
    public ImageProduct(String fileUrl, String mimeType, Long sizeBytes) {
        this.fileUrl = fileUrl;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
    }

    // Convenience constructor with all required fields
    public ImageProduct(String fileUrl, String mimeType, Long sizeBytes, Integer width, Integer height, String altText) {
        this.fileUrl = fileUrl;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
        this.width = width;
        this.height = height;
        this.altText = altText;
    }
}

