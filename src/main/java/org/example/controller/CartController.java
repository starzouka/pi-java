package org.example.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.model.CartItem;
import org.example.model.Cart;
import org.example.service.CartService;
import org.example.util.SessionManager;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * CartController - Optimized cart management with advanced cell factories,
 * async loading, and caching mechanisms for better performance.
 */
public class CartController {
    @FXML
    private TableView<CartItem> cartTable;
    @FXML
    private Label totalLabel;
    @FXML
    private Label itemsCountLabel;
    
    // Column references from FXML
    @FXML
    private TableColumn<CartItem, Integer> imageCol;
    @FXML
    private TableColumn<CartItem, String> nameCol;
    @FXML
    private TableColumn<CartItem, Double> priceCol;
    @FXML
    private TableColumn<CartItem, Integer> quantityCol;
    @FXML
    private TableColumn<CartItem, Double> subtotalCol;
    @FXML
    private TableColumn<CartItem, Void> actionCol;

    // ...existing code...
    private CartService cartService;
    private ObservableList<CartItem> cartItems;
    private int currentUserId = 1;
    private int currentCartId;

    // Caching mechanism
    private Cart cachedCart;
    private long lastCacheUpdateTime = 0;
    private static final long CACHE_DURATION_MS = 5000; // 5 seconds cache

    private static final Logger LOGGER = Logger.getLogger(CartController.class.getName());
    private static final String CURRENCY_FORMAT = "%.2f DT";
    private static final String ARTICLE_SINGULAR = "article";
    private static final String ARTICLE_PLURAL = "articles";

    @FXML
    public void initialize() {
        cartService = new CartService();

        try {
            currentUserId = SessionManager.getInstance().getCurrentUserId();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Impossible de récupérer l'ID utilisateur");
        }

        // Load user cart synchronously (important for cartId)
        loadUserCart();
        
        // Setup UI first
        setupTableColumns();
        
        // Load cart items synchronously on initialize
        loadCartItems();
    }

    private void loadUserCart() {
        try {
            Cart userCart = cartService.getCartByUserId(currentUserId);
            if (userCart != null) {
                currentCartId = userCart.getCartId();
                cachedCart = userCart;
            } else {
                currentCartId = 1;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Erreur lors de la récupération du panier utilisateur", e);
            currentCartId = 1;
        }
    }

    /**
     * Get cached cart or fetch from DB if cache expired
     */
    private Cart getCachedCart() throws SQLException {
        long currentTime = System.currentTimeMillis();
        if (cachedCart != null && (currentTime - lastCacheUpdateTime) < CACHE_DURATION_MS) {
            return cachedCart;
        }
        cachedCart = cartService.getCartById(currentCartId);
        lastCacheUpdateTime = currentTime;
        return cachedCart;
    }

    /**
     * Invalidate cache to force refresh
     */
    private void invalidateCache() {
        cachedCart = null;
        lastCacheUpdateTime = 0;
    }

    private void setupTableColumns() {
        if (cartTable == null) return;
        
        // Column 0: Image (Using product ID as placeholder for image path)
        if (imageCol != null) {
            setupImageColumn(imageCol);
        }
        
        // Column 1: Product Name (Already using PropertyValueFactory in FXML)
        // No special setup needed - default text column
        
        // Column 2: Unit Price
        if (priceCol != null) {
            setupPriceColumn(priceCol);
        }
        
        // Column 3: Quantity (with +/- buttons)
        if (quantityCol != null) {
            setupQuantityColumn(quantityCol);
        }
        
        // Column 4: Subtotal (Already formatted via CSS)
        // No special setup needed
        
        // Column 5: Action (Remove button)
        if (actionCol != null) {
            setupActionColumn(actionCol);
        }
    }

    /**
     * Setup Image Column with ImageView or placeholder
     */
    private void setupImageColumn(TableColumn<CartItem, Integer> imageCol) {
        imageCol.setCellFactory(param -> new TableCell<CartItem, Integer>() {
            private final Label placeholder = new Label("📦");

            {
                placeholder.setStyle("-fx-font-size: 20px; -fx-text-fill: #28ff8a;");
            }

            @Override
            protected void updateItem(Integer productId, boolean empty) {
                super.updateItem(productId, empty);
                if (empty || productId == null) {
                    setGraphic(null);
                } else {
                    // TODO: Load actual product image if available
                    // For now, show placeholder emoji
                    setGraphic(placeholder);
                }
            }
        });
    }

    /**
     * Setup Price Column with proper formatting
     */
    private void setupPriceColumn(TableColumn<CartItem, Double> priceCol) {
        priceCol.setCellFactory(param -> new TableCell<CartItem, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(formatCurrency(price));
                    setStyle("-fx-text-fill: #e9f2ff; -fx-font-weight: 700;");
                }
            }
        });
    }

    /**
     * Setup Quantity Column with +/- buttons
     */
    private void setupQuantityColumn(TableColumn<CartItem, Integer> quantityCol) {
        quantityCol.setCellFactory(param -> new TableCell<CartItem, Integer>() {
            private final Button btnMinus = new Button();
            private final Button btnPlus = new Button();
            private final Label quantityLabel = new Label();
            private final HBox container = new HBox();

            {
                container.setAlignment(Pos.CENTER);
                container.setSpacing(12);
                container.setPadding(new Insets(8));
                container.getChildren().addAll(btnMinus, quantityLabel, btnPlus);

                // Setup MINUS button with text
                btnMinus.setText("−");
                btnMinus.setMinWidth(40);
                btnMinus.setMinHeight(40);
                btnMinus.setPrefWidth(40);
                btnMinus.setPrefHeight(40);
                btnMinus.setStyle(
                    "-fx-font-size: 24px; " +
                    "-fx-text-fill: white; " +
                    "-fx-font-weight: bold; " +
                    "-fx-background-color: #b8452c; " +
                    "-fx-background-radius: 6; " +
                    "-fx-border-radius: 6; " +
                    "-fx-cursor: hand; " +
                    "-fx-padding: 0;"
                );

                // Setup PLUS button with text
                btnPlus.setText("+");
                btnPlus.setMinWidth(40);
                btnPlus.setMinHeight(40);
                btnPlus.setPrefWidth(40);
                btnPlus.setPrefHeight(40);
                btnPlus.setStyle(
                    "-fx-font-size: 24px; " +
                    "-fx-text-fill: white; " +
                    "-fx-font-weight: bold; " +
                    "-fx-background-color: #5a9c3d; " +
                    "-fx-background-radius: 6; " +
                    "-fx-border-radius: 6; " +
                    "-fx-cursor: hand; " +
                    "-fx-padding: 0;"
                );

                // Quantity Label
                quantityLabel.setMinWidth(60);
                quantityLabel.setPrefWidth(60);
                quantityLabel.setMaxWidth(60);
                quantityLabel.setAlignment(Pos.CENTER);
                quantityLabel.setStyle(
                    "-fx-text-fill: white; " +
                    "-fx-font-size: 18px; " +
                    "-fx-font-weight: bold; " +
                    "-fx-text-alignment: center; " +
                    "-fx-alignment: center;"
                );

                // Minus Action
                btnMinus.setOnAction(event -> {
                    if (getIndex() >= 0 && getIndex() < getTableView().getItems().size()) {
                        CartItem item = getTableView().getItems().get(getIndex());
                        int newQuantity = item.getQuantity() - 1;
                        if (newQuantity <= 0) {
                            removeItem(item);
                        } else {
                            updateQuantityAsync(item, newQuantity);
                        }
                    }
                });

                // Plus Action
                btnPlus.setOnAction(event -> {
                    if (getIndex() >= 0 && getIndex() < getTableView().getItems().size()) {
                        CartItem item = getTableView().getItems().get(getIndex());
                        int newQuantity = item.getQuantity() + 1;
                        updateQuantityAsync(item, newQuantity);
                    }
                });
            }

            @Override
            protected void updateItem(Integer quantity, boolean empty) {
                super.updateItem(quantity, empty);
                if (empty || quantity == null || getIndex() < 0) {
                    setGraphic(null);
                } else {
                    quantityLabel.setText(quantity.toString());
                    setGraphic(container);
                }
            }
        });
    }

    /**
     * Setup Action Column with delete button
     */
    private void setupActionColumn(TableColumn<CartItem, Void> actionCol) {
        actionCol.setCellFactory(param -> new TableCell<CartItem, Void>() {
            private final Button btnRemove = new Button("🗑");
            private final HBox pane = new HBox(btnRemove);

            {
                btnRemove.setStyle(
                    "-fx-background-color: transparent; " +
                    "-fx-text-fill: #ff6b6b; " +
                    "-fx-font-size: 16px; " +
                    "-fx-padding: 5; " +
                    "-fx-cursor: hand;"
                );

                // Hover effect
                btnRemove.setOnMouseEntered(e ->
                    btnRemove.setStyle(
                        "-fx-background-color: rgba(255, 107, 107, 0.2); " +
                        "-fx-text-fill: #ff4757; " +
                        "-fx-font-size: 16px; " +
                        "-fx-padding: 5; " +
                        "-fx-cursor: hand; " +
                        "-fx-border-radius: 6; " +
                        "-fx-background-radius: 6;"
                    )
                );
                btnRemove.setOnMouseExited(e ->
                    btnRemove.setStyle(
                        "-fx-background-color: transparent; " +
                        "-fx-text-fill: #ff6b6b; " +
                        "-fx-font-size: 16px; " +
                        "-fx-padding: 5; " +
                        "-fx-cursor: hand;"
                    )
                );

                pane.setAlignment(Pos.CENTER);

                btnRemove.setOnAction(event -> {
                    if (getIndex() >= 0 && getIndex() < getTableView().getItems().size()) {
                        CartItem item = getTableView().getItems().get(getIndex());
                        removeItem(item);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    private void loadCartItems() {
        try {
            List<CartItem> items = cartService.getCartItems(currentCartId);
            cartItems = FXCollections.observableArrayList(items);
            cartTable.setItems(cartItems);
            
            LOGGER.log(Level.INFO, "Panier chargé avec " + items.size() + " articles");
            updateTotal();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du chargement des articles du panier", e);
            showAlert("Erreur", "Impossible de charger le panier: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * Update quantity asynchronously to prevent UI freezing during DB operations
     */
    private void updateQuantityAsync(CartItem item, int newQuantity) {
        if (newQuantity < 1) return; // Validation

        LOGGER.log(Level.INFO, "Mise à jour quantité: " + item.getProductName() + " → " + newQuantity);

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() throws SQLException {
                return cartService.updateCartItemQuantity(
                    item.getCartId(),
                    item.getProductId(),
                    newQuantity
                );
            }

            @Override
            protected void succeeded() {
                Boolean result = getValue();
                if (result != null && result) {
                    // Mettre à jour l'objet local
                    item.setQuantity(newQuantity);
                    invalidateCache();

                    // Forcer le refresh de la table
                    Platform.runLater(() -> {
                        cartTable.refresh();
                        updateTotal();
                        LOGGER.log(Level.INFO, "Quantité mise à jour avec succès");
                    });
                } else {
                    Platform.runLater(() ->
                        showAlert("Erreur", "La quantité n'a pas pu être mise à jour", Alert.AlertType.ERROR)
                    );
                }
            }

            @Override
            protected void failed() {
                LOGGER.log(Level.WARNING, "Erreur lors de la mise à jour de la quantité", getException());
                Platform.runLater(() ->
                    showAlert("Erreur", "Impossible de mettre à jour la quantité: " +
                        getException().getMessage(), Alert.AlertType.ERROR)
                );
            }
        };

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void updateTotal() {
        try {
            double total = cartService.getCartTotal(currentCartId);
            if (totalLabel != null) {
                totalLabel.setText(formatCurrency(total));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Erreur lors du calcul du total", e);
            if (totalLabel != null) {
                totalLabel.setText(formatCurrency(0));
            }
        }
        updateItemCount();
    }

    private void updateItemCount() {
        try {
            int count = cartService.getCartItemCount(currentCartId);
            if (itemsCountLabel != null) {
                itemsCountLabel.setText(count + " " + (count > 1 ? ARTICLE_PLURAL : ARTICLE_SINGULAR));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Erreur lors du comptage des articles", e);
            if (itemsCountLabel != null) {
                itemsCountLabel.setText("0 " + ARTICLE_SINGULAR);
            }
        }
    }

    /**
     * Format price to currency string
     */
    private String formatCurrency(double amount) {
        return String.format(CURRENCY_FORMAT, amount);
    }

    private void removeItem(CartItem item) {
        if (item == null) return;

        // Show confirmation dialog
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText(null);
        confirm.setContentText("Retirer \"" + item.getProductName() + "\" du panier ?");
        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            Task<Boolean> task = new Task<>() {
                @Override
                protected Boolean call() throws SQLException {
                    return cartService.removeFromCart(item.getCartId(), item.getProductId());
                }

                @Override
                protected void succeeded() {
                    if (getValue()) {
                        Platform.runLater(() -> {
                            cartItems.remove(item);
                            invalidateCache();
                            updateTotal();
                        });
                    }
                }

                @Override
                protected void failed() {
                    LOGGER.log(Level.WARNING, "Erreur lors de la suppression", getException());
                    Platform.runLater(() ->
                        showAlert("Erreur", "Impossible de retirer l'article", Alert.AlertType.ERROR)
                    );
                }
            };

            Thread thread = new Thread(task);
            thread.setDaemon(true);
            thread.start();
        }
    }

    @FXML
    private void clearCart() {
        if (cartItems == null || cartItems.isEmpty()) {
            showAlert("Information", "Le panier est déjà vide.", Alert.AlertType.INFORMATION);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer");
        confirm.setHeaderText(null);
        confirm.setContentText("Êtes-vous sûr de vouloir vider complètement le panier ?");
        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            Task<Boolean> task = new Task<>() {
                @Override
                protected Boolean call() throws SQLException {
                    return cartService.clearCart(currentCartId);
                }

                @Override
                protected void succeeded() {
                    if (getValue()) {
                        Platform.runLater(() -> {
                            cartItems.clear();
                            invalidateCache();
                            updateTotal();
                            showAlert("Succès", "Panier vidé avec succès.", Alert.AlertType.INFORMATION);
                        });
                    }
                }

                @Override
                protected void failed() {
                    LOGGER.log(Level.WARNING, "Erreur lors du vidage du panier", getException());
                    Platform.runLater(() ->
                        showAlert("Erreur", "Impossible de vider le panier", Alert.AlertType.ERROR)
                    );
                }
            };

            Thread thread = new Thread(task);
            thread.setDaemon(true);
            thread.start();
        }
    }

    @FXML
    private void checkout() {
        if (cartItems == null || cartItems.isEmpty()) {
            showAlert("Attention", "Votre panier est vide", Alert.AlertType.WARNING);
            return;
        }
        navigateTo("/fxml/Front/Checkout.fxml");
    }

    @FXML
    private void goBack() {
        navigateTo("/fxml/Front/ListProducts.fxml");
    }

    private void navigateTo(String fxmlPath) {
        Task<Parent> loadTask = new Task<>() {
            @Override
            protected Parent call() throws IOException {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                return loader.load();
            }

            @Override
            protected void succeeded() {
                try {
                    Parent root = getValue();
                    Scene scene = new Scene(root, 1280, 720);
                    scene.getStylesheets().add(getClass().getResource("/css/style_front.css").toExternalForm());
                    Stage stage = (Stage) cartTable.getScene().getWindow();
                    stage.setScene(scene);
                    stage.centerOnScreen();
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Erreur lors du changement de scène", e);
                }
            }

            @Override
            protected void failed() {
                LOGGER.log(Level.SEVERE, "Erreur lors du chargement du fichier FXML", getException());
                Platform.runLater(() ->
                    showAlert("Erreur Navigation", "Impossible de charger " + fxmlPath, Alert.AlertType.ERROR)
                );
            }
        };

        Thread thread = new Thread(loadTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
