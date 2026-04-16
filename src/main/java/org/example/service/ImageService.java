package org.example.service;

import org.example.connection.MyConnection;
import org.example.model.ImageProduct;
import org.example.model.ProductImage;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service pour gérer les images des produits
 */
public class ImageService {
    private Connection connection;

    public ImageService() {
        this.connection = MyConnection.getInstance().getConnection();
    }

    /**
     * Récupérer l'image principale d'un produit (première image)
     */
    public ImageProduct getMainImageByProductId(int productId) throws SQLException {
        String query = """
            SELECT i.* FROM images i
            JOIN product_images pi ON i.image_id = pi.image_id
            WHERE pi.product_id = ?
            ORDER BY pi.position ASC
            LIMIT 1
        """;

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, productId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToImage(rs);
                }
            }
        }
        return null;
    }

    /**
     * Récupérer toutes les images d'un produit
     */
    public List<ImageProduct> getImagesByProductId(int productId) throws SQLException {
        List<ImageProduct> images = new ArrayList<>();
        String query = """
            SELECT i.* FROM images i
            JOIN product_images pi ON i.image_id = pi.image_id
            WHERE pi.product_id = ?
            ORDER BY pi.position ASC
        """;

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, productId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    images.add(mapRowToImage(rs));
                }
            }
        }
        return images;
    }

    /**
     * Ajouter une image à un produit
     */
    public boolean addImageToProduct(int productId, int imageId, int position) throws SQLException {
        String query = "INSERT INTO product_images (product_id, image_id, position) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, productId);
            stmt.setInt(2, imageId);
            stmt.setInt(3, position);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Créer une nouvelle image
     */
    public int createImage(String fileUrl, String mimeType, Long sizeBytes, Integer width, Integer height, String altText, Integer uploadedByUserId) throws SQLException {
        String query = "INSERT INTO images (file_url, mime_type, size_bytes, width, height, alt_text, uploaded_by_user_id, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, NOW())";
        try (PreparedStatement stmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, fileUrl);
            stmt.setString(2, mimeType);
            stmt.setLong(3, sizeBytes);

            // Gérer les valeurs nullables pour width et height
            if (width != null) {
                stmt.setInt(4, width);
            } else {
                stmt.setNull(4, Types.INTEGER);
            }

            if (height != null) {
                stmt.setInt(5, height);
            } else {
                stmt.setNull(5, Types.INTEGER);
            }

            stmt.setString(6, altText);

            if (uploadedByUserId != null) {
                stmt.setInt(7, uploadedByUserId);
            } else {
                stmt.setNull(7, Types.INTEGER);
            }

            if (stmt.executeUpdate() > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        }
        return -1;
    }

    /**
     * Supprimer une image d'un produit
     */
    public boolean removeImageFromProduct(int productId, int imageId) throws SQLException {
        String query = "DELETE FROM product_images WHERE product_id = ? AND image_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, productId);
            stmt.setInt(2, imageId);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Supprimer complètement une image (de la table images et product_images)
     */
    public boolean deleteImage(int imageId) throws SQLException {
        try {
            connection.setAutoCommit(false);

            // D'abord supprimer les associations
            String deleteAssociations = "DELETE FROM product_images WHERE image_id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(deleteAssociations)) {
                stmt.setInt(1, imageId);
                stmt.executeUpdate();
            }

            // Ensuite supprimer l'image
            String deleteImage = "DELETE FROM images WHERE image_id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(deleteImage)) {
                stmt.setInt(1, imageId);
                boolean result = stmt.executeUpdate() > 0;
                connection.commit();
                return result;
            }
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    /**
     * Obtenir une image par ID
     */
    public ImageProduct getImageById(int imageId) throws SQLException {
        String query = "SELECT * FROM images WHERE image_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, imageId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToImage(rs);
                }
            }
        }
        return null;
    }

    /**
     * Mapper une ligne ResultSet vers un objet ImageProduct
     */
    private ImageProduct mapRowToImage(ResultSet rs) throws SQLException {
        ImageProduct image = new ImageProduct();
        image.setImageId(rs.getInt("image_id"));
        image.setFileUrl(rs.getString("file_url"));
        image.setMimeType(rs.getString("mime_type"));
        image.setSizeBytes(rs.getLong("size_bytes"));

        // Gérer les valeurs nullables pour width et height
        Object widthObj = rs.getObject("width");
        if (widthObj != null) {
            image.setWidth((Integer) widthObj);
        }

        Object heightObj = rs.getObject("height");
        if (heightObj != null) {
            image.setHeight((Integer) heightObj);
        }

        image.setAltText(rs.getString("alt_text"));
        image.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));

        Object uploadedByUserIdObj = rs.getObject("uploaded_by_user_id");
        if (uploadedByUserIdObj != null) {
            image.setUploadedByUserId((Integer) uploadedByUserIdObj);
        }

        return image;
    }
}

