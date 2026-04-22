package com.pulse.desktop.ui;

import com.pulse.desktop.auth.SessionContext;
import com.pulse.desktop.auth.SessionUser;
import com.pulse.desktop.db.Jdbc;
import com.pulse.desktop.model.LookupItem;
import com.pulse.desktop.model.RouteDefinition;
import com.pulse.desktop.service.ExportService;
import com.pulse.desktop.service.Navigator;
import com.pulse.desktop.util.AlertUtils;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AdminCartsController implements RouteAwarePage {
    private static final DateTimeFormatter DATE_TIME_INPUT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    private Label formTitleLabel;
    @FXML
    private ComboBox<LookupItem> userCombo;
    @FXML
    private ComboBox<String> statusCombo;
    @FXML
    private TextField lockedAtField;
    @FXML
    private Label formFeedbackLabel;
    @FXML
    private Button saveButton;
    @FXML
    private Button cancelEditButton;

    @FXML
    private TextField qField;
    @FXML
    private ComboBox<String> statusFilterCombo;
    @FXML
    private ComboBox<String> sortCombo;
    @FXML
    private ComboBox<String> directionCombo;
    @FXML
    private Label resultCountLabel;
    @FXML
    private VBox cartsListBox;

    private final ExportService exportService = new ExportService();
    private Integer editingCartId;
    private List<CartRow> currentRows = List.of();

    @FXML
    public void initialize() {
        statusCombo.setItems(FXCollections.observableArrayList("OPEN", "LOCKED", "ORDERED"));
        statusCombo.getSelectionModel().select("OPEN");

        statusFilterCombo.setItems(FXCollections.observableArrayList("", "OPEN", "LOCKED", "ORDERED"));
        statusFilterCombo.getSelectionModel().select(0);

        sortCombo.setItems(FXCollections.observableArrayList("updated_at", "id", "user", "status", "items", "created_at", "locked_at"));
        sortCombo.getSelectionModel().select("updated_at");

        directionCombo.setItems(FXCollections.observableArrayList("desc", "asc"));
        directionCombo.getSelectionModel().select("desc");

        resetForm();
    }

    @Override
    public void setRoute(RouteDefinition routeDefinition) {
        SessionUser admin = requireAdmin();
        if (admin == null) {
            return;
        }
        loadUsers();
        resetForm();
        refresh();
    }

    @FXML
    private void saveCart() {
        SessionUser admin = requireAdmin();
        if (admin == null) {
            return;
        }

        LookupItem user = userCombo.getValue();
        if (user == null || user.getId() <= 0) {
            formFeedbackLabel.setText("Utilisateur invalide.");
            return;
        }

        String status = safe(statusCombo.getValue()).toUpperCase();
        if (!List.of("OPEN", "LOCKED", "ORDERED").contains(status)) {
            status = "OPEN";
        }

        LocalDateTime lockedAt = parseDateTime(lockedAtField.getText());
        if (!safe(lockedAtField.getText()).isBlank() && lockedAt == null) {
            formFeedbackLabel.setText("Date locked invalide (yyyy-MM-dd HH:mm).");
            return;
        }

        try (Connection connection = Jdbc.open()) {
            if (editingCartId == null) {
                String sql = """
                        INSERT INTO carts (user_id, status, created_at, updated_at, locked_at)
                        VALUES (?, ?, NOW(), NOW(), ?)
                        """;
                try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setInt(1, user.getId());
                    statement.setString(2, status);
                    if (lockedAt == null) {
                        statement.setTimestamp(3, null);
                    } else {
                        statement.setTimestamp(3, Timestamp.valueOf(lockedAt));
                    }
                    statement.executeUpdate();
                }
                AlertUtils.info("Admin paniers", "Panier cree.");
            } else {
                String sql = """
                        UPDATE carts
                        SET user_id = ?, status = ?, locked_at = ?, updated_at = NOW()
                        WHERE cart_id = ?
                        """;
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setInt(1, user.getId());
                    statement.setString(2, status);
                    if (lockedAt == null) {
                        statement.setTimestamp(3, null);
                    } else {
                        statement.setTimestamp(3, Timestamp.valueOf(lockedAt));
                    }
                    statement.setInt(4, editingCartId);
                    statement.executeUpdate();
                }
                AlertUtils.info("Admin paniers", "Panier mis a jour.");
            }
            resetForm();
            refresh();
        } catch (SQLException ex) {
            AlertUtils.error("Admin paniers", "Enregistrement impossible (utilisateur deja associe ou liaison invalide).\n" + ex.getMessage());
        }
    }

    @FXML
    private void cancelEdit() {
        resetForm();
    }

    @FXML
    private void applyFilters() {
        refresh();
    }

    @FXML
    private void resetFilters() {
        qField.clear();
        statusFilterCombo.getSelectionModel().select(0);
        sortCombo.getSelectionModel().select("updated_at");
        directionCombo.getSelectionModel().select("desc");
        refresh();
    }

    @FXML
    private void exportPdf() {
        export(true);
    }

    @FXML
    private void exportExcel() {
        export(false);
    }

    private void loadUsers() {
        List<LookupItem> users = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT user_id, CONCAT(username, ' (', email, ')') AS label
                     FROM users
                     ORDER BY username ASC
                     LIMIT 800
                     """);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                users.add(new LookupItem(rs.getInt("user_id"), rs.getString("label")));
            }
        } catch (SQLException ex) {
            AlertUtils.error("Admin paniers", "Impossible de charger les utilisateurs.\n" + ex.getMessage());
        }

        userCombo.setItems(FXCollections.observableArrayList(users));
        if (!users.isEmpty()) {
            userCombo.getSelectionModel().select(0);
        }
    }

    private void refresh() {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    c.cart_id,
                    c.user_id,
                    u.username,
                    u.email,
                    c.status,
                    c.created_at,
                    c.updated_at,
                    c.locked_at,
                    COALESCE(SUM(ci.quantity), 0) AS items_count
                FROM carts c
                LEFT JOIN users u ON u.user_id = c.user_id
                LEFT JOIN cart_items ci ON ci.cart_id = c.cart_id
                WHERE 1=1
                """);

        List<Object> params = new ArrayList<>();

        String q = safe(qField.getText());
        if (!q.isBlank()) {
            String like = "%" + q.toLowerCase() + "%";
            sql.append("""
                     AND (
                        LOWER(COALESCE(u.username, '')) LIKE ?
                        OR LOWER(COALESCE(u.email, '')) LIKE ?
                        OR LOWER(COALESCE(u.display_name, '')) LIKE ?
                     )
                    """);
            params.add(like);
            params.add(like);
            params.add(like);
        }

        String status = safe(statusFilterCombo.getValue()).toUpperCase();
        if (!status.isBlank()) {
            sql.append(" AND c.status = ? ");
            params.add(status);
        }

        sql.append(" GROUP BY c.cart_id, c.user_id, u.username, u.email, c.status, c.created_at, c.updated_at, c.locked_at ");

        String direction = "asc".equalsIgnoreCase(safe(directionCombo.getValue())) ? "ASC" : "DESC";
        String sort = safe(sortCombo.getValue());
        switch (sort) {
            case "id" -> sql.append(" ORDER BY c.cart_id ").append(direction);
            case "user" -> sql.append(" ORDER BY u.username ").append(direction).append(", c.cart_id DESC");
            case "status" -> sql.append(" ORDER BY c.status ").append(direction).append(", c.cart_id DESC");
            case "items" -> sql.append(" ORDER BY items_count ").append(direction).append(", c.cart_id DESC");
            case "created_at" -> sql.append(" ORDER BY c.created_at ").append(direction).append(", c.cart_id DESC");
            case "locked_at" -> sql.append(" ORDER BY c.locked_at ").append(direction).append(", c.cart_id DESC");
            default -> sql.append(" ORDER BY c.updated_at ").append(direction).append(", c.cart_id DESC");
        }
        sql.append(" LIMIT 500 ");

        List<CartRow> rows = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bind(statement, params);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(new CartRow(
                            rs.getInt("cart_id"),
                            rs.getInt("user_id"),
                            rs.getString("username"),
                            rs.getString("email"),
                            rs.getString("status"),
                            rs.getInt("items_count"),
                            toLocalDateTime(rs.getTimestamp("created_at")),
                            toLocalDateTime(rs.getTimestamp("updated_at")),
                            toLocalDateTime(rs.getTimestamp("locked_at"))
                    ));
                }
            }
        } catch (SQLException ex) {
            AlertUtils.error("Admin paniers", "Chargement impossible.\n" + ex.getMessage());
            return;
        }

        currentRows = rows;
        resultCountLabel.setText(rows.size() + " resultat(s)");
        renderRows();
    }

    private void renderRows() {
        cartsListBox.getChildren().clear();
        if (currentRows.isEmpty()) {
            cartsListBox.getChildren().add(CompetitionUi.emptyState("Aucun panier trouve."));
            return;
        }

        for (CartRow row : currentRows) {
            String line1 = "#" + row.cartId() + " | " + CompetitionUi.emptySafe(row.username()) + " | " + CompetitionUi.emptySafe(row.status());
            String line2 = "Items: " + row.itemsCount()
                    + " | Created: " + CompetitionUi.fmtDateTime(row.createdAt())
                    + " | Updated: " + CompetitionUi.fmtDateTime(row.updatedAt())
                    + " | Locked: " + CompetitionUi.fmtDateTime(row.lockedAt());

            Button update = new Button("Update");
            update.getStyleClass().add("btn-ghost");
            update.setOnAction(event -> startEdit(row));

            Button delete = new Button("Delete");
            delete.getStyleClass().add("btn-ghost");
            delete.setOnAction(event -> deleteCart(row.cartId()));

            cartsListBox.getChildren().add(CompetitionUi.listRowWithActions(line1, line2, update, delete));
        }
    }

    private void startEdit(CartRow row) {
        editingCartId = row.cartId();
        formTitleLabel.setText("MODIFIER PANIER #" + row.cartId());
        saveButton.setText("Mettre a jour");
        cancelEditButton.setVisible(true);
        cancelEditButton.setManaged(true);

        selectById(userCombo, row.userId());
        statusCombo.getSelectionModel().select(row.status());
        lockedAtField.setText(row.lockedAt() == null ? "" : row.lockedAt().format(DATE_TIME_INPUT));
        formFeedbackLabel.setText("Mode edition actif.");
    }

    private void deleteCart(int cartId) {
        if (!confirmDelete()) {
            return;
        }

        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM carts WHERE cart_id = ?")) {
            statement.setInt(1, cartId);
            int affected = statement.executeUpdate();
            if (affected <= 0) {
                AlertUtils.warning("Admin paniers", "Panier introuvable.");
                return;
            }
            if (editingCartId != null && editingCartId == cartId) {
                resetForm();
            }
            AlertUtils.info("Admin paniers", "Panier supprime.");
            refresh();
        } catch (SQLException ex) {
            AlertUtils.error("Admin paniers", "Suppression impossible (panier lie a une commande).\n" + ex.getMessage());
        }
    }

    private void export(boolean pdf) {
        if (currentRows.isEmpty()) {
            AlertUtils.info("Export", "Aucune donnee a exporter.");
            return;
        }

        List<String> headers = List.of("ID", "User", "Status", "Items", "Created at", "Updated at", "Locked at");
        List<List<String>> rows = new ArrayList<>();
        for (CartRow row : currentRows) {
            rows.add(List.of(
                    Integer.toString(row.cartId()),
                    CompetitionUi.emptySafe(row.username()),
                    CompetitionUi.emptySafe(row.status()),
                    Integer.toString(row.itemsCount()),
                    CompetitionUi.fmtDateTime(row.createdAt()),
                    CompetitionUi.fmtDateTime(row.updatedAt()),
                    CompetitionUi.fmtDateTime(row.lockedAt())
            ));
        }

        try {
            var file = pdf
                    ? exportService.exportPdf("Paniers", headers, rows)
                    : exportService.exportExcel("admin_carts", headers, rows);
            exportService.openFile(file);
            AlertUtils.info("Export", "Fichier genere: " + file.toAbsolutePath());
        } catch (IOException ex) {
            AlertUtils.error("Export", "Impossible de generer l'export.\n" + ex.getMessage());
        }
    }

    private void resetForm() {
        editingCartId = null;
        formTitleLabel.setText("NOUVEAU PANIER");
        saveButton.setText("Creer panier");
        cancelEditButton.setVisible(false);
        cancelEditButton.setManaged(false);
        statusCombo.getSelectionModel().select("OPEN");
        lockedAtField.clear();
        formFeedbackLabel.setText("");
        if (userCombo.getValue() == null && !userCombo.getItems().isEmpty()) {
            userCombo.getSelectionModel().select(0);
        }
    }

    private boolean confirmDelete() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Suppression panier");
        alert.setHeaderText(null);
        alert.setContentText("Supprimer ce panier ?");
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private static SessionUser requireAdmin() {
        SessionUser user = SessionContext.getCurrentUser();
        if (user == null) {
            AlertUtils.warning("Connexion requise", "Connectez-vous en admin.");
            Navigator.goTo("front_login");
            return null;
        }
        if (!"ADMIN".equals(SessionContext.currentRole())) {
            AlertUtils.warning("Acces refuse", "Cette page est reservee a l'administration.");
            Navigator.goTo("front_home");
            return null;
        }
        return user;
    }

    private static void bind(PreparedStatement statement, List<Object> params) throws SQLException {
        int index = 1;
        for (Object value : params) {
            if (value instanceof Integer intValue) {
                statement.setInt(index++, intValue);
            } else if (value instanceof String text) {
                statement.setString(index++, text);
            } else {
                statement.setObject(index++, value);
            }
        }
    }

    private static void selectById(ComboBox<LookupItem> combo, int id) {
        for (LookupItem item : combo.getItems()) {
            if (item.getId() == id) {
                combo.getSelectionModel().select(item);
                return;
            }
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static LocalDateTime parseDateTime(String raw) {
        String value = safe(raw);
        if (value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, DATE_TIME_INPUT);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private record CartRow(
            int cartId,
            int userId,
            String username,
            String email,
            String status,
            int itemsCount,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime lockedAt
    ) {
    }
}
