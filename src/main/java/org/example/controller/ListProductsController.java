package org.example.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.example.model.Product;
import org.example.model.Cart;
import org.example.model.ImageProduct;
import org.example.service.ProductService;
import org.example.service.CartService;
import org.example.service.ImageService;
import org.example.util.SessionManager;

import java.io.IOException;
import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Contrôleur pour l'affichage de la liste des produits (Front)
 */
public class ListProductsController {
    @FXML private TextField searchField;
    @FXML private ComboBox<String> teamFilter;
    @FXML private FlowPane productGrid;
    @FXML private Button cartButton;

    private ProductService productService;
    private CartService cartService;
    private ImageService imageService;
    private Cart userCart;
    private ObservableList<Product> productList;
    private int currentUserId = 1;
    private static final Logger LOGGER = Logger.getLogger(ListProductsController.class.getName());

    @FXML
    public void initialize() {
        try {
            currentUserId = SessionManager.getInstance().getCurrentUserId();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Impossible de récupérer l'ID utilisateur, utilisation de la valeur par défaut");
        }

        productService = new ProductService();
        cartService = new CartService();
        imageService = new ImageService();
        
        try {
            loadUserCart();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Impossible de charger le panier: " + e.getMessage());
            userCart = new Cart();
        }
        
        loadProducts();
        setupTeamFilter();
        updateCartButton();
    }

    /**
     * Charger le panier de l'utilisateur
     */
    private void loadUserCart() {
        try {
            userCart = cartService.getCartByUserId(currentUserId);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Erreur lors du chargement du panier: " + e.getMessage());
            userCart = new Cart();
        }
    }

    /**
     * Charger tous les produits
     */
    private void loadProducts() {
        try {
            List<Product> products = productService.getActive(); // Charger seulement les produits actifs
            for (Product product : products) {
                try {
                    ImageProduct mainImage = imageService.getMainImageByProductId(product.getProductId());
                    if (mainImage != null) {
                        product.setImageUrl(mainImage.getFileUrl());
                    }
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Erreur lors du chargement de l'image du produit " + product.getProductId() + ": " + e.getMessage());
                }
            }
            productList = FXCollections.observableArrayList(products);
            displayProducts();
            LOGGER.log(Level.INFO, "Produits chargés: " + products.size());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du chargement des produits: " + e.getMessage());
            showAlert("Erreur", "Impossible de charger les produits: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * Configurer le filtre par team
     */
    private void setupTeamFilter() {
        teamFilter.setItems(FXCollections.observableArrayList(
            "Toutes les équipes", "Team Alpha", "Team Pulse", "Team Beta"
        ));
        teamFilter.setValue("Toutes les équipes");
        teamFilter.setOnAction(event -> filterProducts());
    }

    /**
     * Afficher les produits
     */
    private void displayProducts() {
        productGrid.getChildren().clear();
        if (productList == null || productList.isEmpty()) {
            Label noProducts = new Label("Aucun produit disponible");
            noProducts.setStyle("-fx-font-size: 16px; -fx-text-fill: #999;");
            productGrid.getChildren().add(noProducts);
            return;
        }
        for (Product product : productList) {
            productGrid.getChildren().add(createProductCard(product));
        }
    }

     /**
     * Créer une carte produit
     */
    private VBox createProductCard(Product product) {
        VBox card = new VBox();
        card.getStyleClass().add("card-product");
        card.setSpacing(0);

        // Image Section
        StackPane mediaPane = new StackPane();
        mediaPane.getStyleClass().add("card-media");
        mediaPane.setStyle("-fx-background-color: rgba(255,255,255,0.03);");

        ImageView imageView = new ImageView();
        imageView.setFitWidth(280);
        imageView.setFitHeight(180);
        imageView.setPreserveRatio(false);
        
        // Créer un clip avec des coins arrondis pour l'image
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(280, 180);
        clip.setArcWidth(22);
        clip.setArcHeight(22);
        imageView.setClip(clip);

        // Charger l'image si disponible
        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            try {
                String imagePath = product.getImageUrl();
                String imageUrl;

                if (imagePath.startsWith("uploads/")) {
                    String basePath = new File("").getAbsolutePath();
                    String fullPath = basePath + File.separator + imagePath;
                    imageUrl = new File(fullPath).toURI().toString();
                } else {
                    imageUrl = imagePath;
                }

                Image image = new Image(imageUrl, 280, 180, false, true);
                imageView.setImage(image);
                LOGGER.log(Level.FINE, "Image chargée: " + product.getName());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Erreur chargement image " + product.getProductId() + ": " + e.getMessage());
                addPlaceholderImage(mediaPane);
            }
        } else {
            addPlaceholderImage(mediaPane);
        }
        
        // Prix Badge
        Label priceChip = new Label(String.format("%.2f €", product.getPrice()));
        priceChip.getStyleClass().addAll("chip", "chip-price");
        StackPane.setAlignment(priceChip, Pos.TOP_RIGHT);
        StackPane.setMargin(priceChip, new Insets(10));

        mediaPane.getChildren().addAll(imageView, priceChip);

        // Body Section
        VBox body = new VBox(15);
        body.getStyleClass().add("card-body");

        Label name = new Label(product.getName());
        name.getStyleClass().add("card-title");
        
        Label desc = new Label(product.getDescription() != null ? product.getDescription() : "");
        desc.getStyleClass().add("card-desc");
        desc.setPrefHeight(40);
        desc.setWrapText(true);

        HBox meta = new HBox(10);
        Label stockChip = new Label("Stock: " + product.getStockQty());
        stockChip.getStyleClass().add("chip");
        meta.getChildren().add(stockChip);

        Button addBtn = new Button("AJOUTER AU PANIER");
        addBtn.getStyleClass().add("btn-signin");
        addBtn.setMaxWidth(Double.MAX_VALUE);
        addBtn.setOnAction(e -> addToCart(product));
        
        if (product.getStockQty() <= 0) {
            addBtn.setDisable(true);
            addBtn.setText("RUPTURE");
        }

        body.getChildren().addAll(name, desc, meta, addBtn);
        card.getChildren().addAll(mediaPane, body);
        
        return card;
    }

    /**
     * Ajouter une image placeholder quand l'image ne se charge pas
     */
    private void addPlaceholderImage(StackPane mediaPane) {
        Label placeholder = new Label("📷");
        placeholder.setStyle("-fx-font-size: 48px; -fx-text-fill: rgba(255,255,255,0.2);");
        mediaPane.getChildren().add(placeholder);
    }

    /**
     * Filtrer les produits
     */
    @FXML
    public void filterProducts() {
        String searchText = searchField.getText().toLowerCase().trim();
        String selectedTeam = teamFilter.getValue();
        productGrid.getChildren().clear();

        if (productList == null || productList.isEmpty()) {
            displayProducts();
            return;
        }

        boolean hasResults = false;

        for (Product product : productList) {
            // Filtre recherche
            boolean matchesSearch = product.getName().toLowerCase().contains(searchText) ||
                    (product.getDescription() != null && product.getDescription().toLowerCase().contains(searchText));

            // Filtre team
            boolean matchesTeam = selectedTeam == null || selectedTeam.equals("Toutes les équipes");
            if (!matchesTeam) {
                // Correspondance entre le nom de team et l'ID (basé sur database_sample_data.sql)
                switch (selectedTeam) {
                    case "Team Alpha" -> matchesTeam = (product.getTeamId() == 1);
                    case "Team Beta" -> matchesTeam = (product.getTeamId() == 2);
                    case "Team Pulse" -> matchesTeam = (product.getTeamId() == 3);
                    default -> matchesTeam = false;
                }
            }

            if (matchesSearch && matchesTeam) {
                productGrid.getChildren().add(createProductCard(product));
                hasResults = true;
            }
        }

        if (!hasResults) {
            Label noResults = new Label("Aucun produit ne correspond à votre recherche");
            noResults.setStyle("-fx-font-size: 14px; -fx-text-fill: #999;");
            productGrid.getChildren().add(noResults);
        }
    }

    /**
     * Réinitialiser les filtres
     */
    @FXML
    public void resetFilters() {
        searchField.clear();
        teamFilter.setValue("Toutes les équipes");
        displayProducts();
    }

    /**
     * Ajouter un produit au panier
     */
    @FXML
    private void addToCart(Product selectedProduct) {
        if (selectedProduct == null || selectedProduct.getStockQty() <= 0) {
            showAlert("Attention", "Ce produit n'est pas disponible", Alert.AlertType.WARNING);
            return;
        }

        TextInputDialog dialog = new TextInputDialog("1");
        dialog.setTitle("Ajouter au panier");
        dialog.setHeaderText("Quantité pour " + selectedProduct.getName());
        dialog.setContentText("Quantité (max: " + selectedProduct.getStockQty() + "):");

        dialog.showAndWait().ifPresent(quantity -> {
            try {
                int qty = Integer.parseInt(quantity);
                if (qty > 0 && qty <= selectedProduct.getStockQty()) {
                    if (cartService.addToCart(userCart.getCartId(), selectedProduct.getProductId(), qty, selectedProduct.getPrice())) {
                        showAlert("Succès", "Produit ajouté au panier!", Alert.AlertType.INFORMATION);
                        LOGGER.log(Level.INFO, "Produit ajouté au panier: " + selectedProduct.getName() + " (x" + qty + ")");
                        updateCartButton();
                    } else {
                        showAlert("Erreur", "Impossible d'ajouter le produit au panier", Alert.AlertType.ERROR);
                    }
                } else {
                    showAlert("Erreur", "Quantité invalide. Maximum: " + selectedProduct.getStockQty(), Alert.AlertType.ERROR);
                }
            } catch (NumberFormatException e) {
                showAlert("Erreur", "Veuillez entrer un nombre valide", Alert.AlertType.ERROR);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Erreur lors de l'ajout au panier: " + e.getMessage());
                showAlert("Erreur", "Erreur: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    /**
     * Mettre à jour le bouton Panier avec le nombre d'articles
     */
    private void updateCartButton() {
        try {
            int cartCount = cartService.getCartItemCount(userCart.getCartId());
            if (cartButton != null) {
                cartButton.setText("Panier (" + cartCount + ")");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Erreur lors du comptage des articles du panier: " + e.getMessage());
        }
    }

    // Navigation methods
    @FXML private void goToHome() { navigateTo("/fxml/Front/Home.fxml"); }
    @FXML private void goToTournaments() { navigateTo("/fxml/Front/Tournaments.fxml"); }
    @FXML private void goToGames() { navigateTo("/fxml/Front/Games.fxml"); }
    @FXML private void goToMatches() { navigateTo("/fxml/Front/Matches.fxml"); }
    @FXML private void goToTeams() { navigateTo("/fxml/Front/Teams.fxml"); }
    @FXML private void goToCart() { navigateTo("/fxml/Front/Cart.fxml"); }
    @FXML private void goToOrders() { navigateTo("/fxml/Front/Orders.fxml"); }
    @FXML private void goToAdmin() { navigateTo("/fxml/Admin/CRUDProduct.fxml"); }

    /**
     * Naviguer vers une page FXML
     */
    private void navigateTo(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1200, 700);

            String styleSheet = fxmlPath.contains("/Admin/") ?
                    "/css/style.css" : "/css/style_front.css";
            scene.getStylesheets().add(getClass().getResource(styleSheet).toExternalForm());

            Stage stage = (Stage) productGrid.getScene().getWindow();
            stage.setScene(scene);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du chargement de la page: " + e.getMessage());
            showAlert("Erreur", "Impossible de charger la page: " + fxmlPath + "\nErreur: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * Afficher une alerte
     */
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
