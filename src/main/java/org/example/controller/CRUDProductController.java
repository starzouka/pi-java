package org.example.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import org.example.model.Product;
import org.example.model.Team;
import org.example.service.ProductService;
import org.example.service.TeamService;
import org.example.service.ImageService;

import java.io.IOException;
import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Contrôleur pour la gestion CRUD des produits (Admin)
 */
public class CRUDProductController {
    @FXML private TextField nameField;
    @FXML private TextArea descriptionField;
    @FXML private TextField priceField;
    @FXML private TextField stockField;
    @FXML private TextField skuField;
    @FXML private ComboBox<String> teamIdField;
    @FXML private CheckBox activeCheckBox;
    @FXML private TableView<Product> productTable;
    @FXML private ImageView imagePreview;
    @FXML private Label imageNameLabel;
    @FXML private Button selectImageButton;
    @FXML private Label descriptionCounter;

    private ProductService productService;
    private TeamService teamService;
    private ImageService imageService;
    private ObservableList<Product> productList;
    private ObservableList<Team> teamList;
    private Product selectedProduct;
    private File selectedImageFile;
    private static final Logger LOGGER = Logger.getLogger(CRUDProductController.class.getName());

    // Constantes de validation
    private static final int NAME_MAX_LENGTH = 150;
    private static final int SKU_MAX_LENGTH = 64;
    private static final double MIN_PRICE = 0.0;
    private static final double MAX_PRICE = 999999.99;
    private static final int MAX_STOCK = 999999;
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5 MB

    @FXML
    public void initialize() {
        productService = new ProductService();
        teamService = new TeamService();
        imageService = new ImageService();
        loadProducts();
        setupTableColumns();
        // Load available teams on initialization
        loadAvailableTeams();
        initializeImagePreview();
        initializeDescriptionCounter();
    }

    /**
     * Initialiser l'aperçu de l'image
     */
    private void initializeImagePreview() {
        if (imagePreview != null) {
            imagePreview.setFitWidth(150);
            imagePreview.setFitHeight(150);
            imagePreview.setPreserveRatio(true);
            imagePreview.setStyle("-fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 10; -fx-padding: 10;");
        }
        if (imageNameLabel != null) {
            imageNameLabel.setText("Aucune image sélectionnée");
        }
    }

    /**
     * Initialiser le compteur de caractères pour la description
     */
    private void initializeDescriptionCounter() {
        if (descriptionField != null && descriptionCounter != null) {
            // Mettre à jour le compteur lors de la saisie
            descriptionField.textProperty().addListener((obs, oldText, newText) -> {
                updateDescriptionCounter();
            });
            // Initialiser le compteur
            updateDescriptionCounter();
        }
    }

    /**
     * Mettre à jour le compteur de caractères
     */
    private void updateDescriptionCounter() {
        if (descriptionCounter != null && descriptionField != null) {
            int length = descriptionField.getText() != null ? descriptionField.getText().length() : 0;
            descriptionCounter.setText(length + " caractère" + (length > 1 ? "s" : ""));
            
            // Changer la couleur si on dépasse une certaine limite
            if (length > 500) {
                descriptionCounter.setStyle("-fx-font-size: 10px; -fx-text-fill: #ff9d2e;");
            } else if (length > 1000) {
                descriptionCounter.setStyle("-fx-font-size: 10px; -fx-text-fill: #ff6b6b;");
            } else {
                descriptionCounter.setStyle("-fx-font-size: 10px; -fx-text-fill: rgba(255,255,255,0.35);");
            }
        }
    }

    /**
     * Charger les équipes disponibles et afficher un message si aucune n'existe
     */
    private void loadAvailableTeams() {
        try {
            teamList = FXCollections.observableArrayList(teamService.getAll());
            if (teamList.isEmpty()) {
                LOGGER.log(Level.WARNING, "Aucune équipe disponible dans la base de données");
                showAlert("Attention", "Aucune équipe disponible. Veuillez d'abord créer une équipe avant d'ajouter des produits.", Alert.AlertType.WARNING);
            } else {
                // Remplir le ComboBox avec les noms des teams au format "ID - Nom"
                ObservableList<String> teamOptions = FXCollections.observableArrayList();
                for (Team team : teamList) {
                    teamOptions.add(team.getTeamId() + " - " + team.getName());
                }
                teamIdField.setItems(teamOptions);
                LOGGER.log(Level.INFO, "Équipes chargées: " + teamList.size());
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du chargement des équipes: " + e.getMessage());
            showAlert("Erreur", "Impossible de charger les équipes: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * Configurer les colonnes du tableau
     */
    private void setupTableColumns() {
        productTable.setRowFactory(tv -> {
            TableRow<Product> row = new TableRow<Product>() {
                @Override
                protected void updateItem(Product item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item != null && !empty) {
                        setOnMouseClicked(event -> selectProduct(item));
                    }
                }
            };
            return row;
        });
    }

    /**
     * Sélectionner un produit et remplir le formulaire
     */
    private void selectProduct(Product product) {
        if (product == null) {
            LOGGER.log(Level.WARNING, "Tentative de sélection d'un produit null");
            return;
        }
        selectedProduct = product;
        populateForm(product);
    }

    /**
     * Remplir le formulaire avec les données du produit
     */
    private void populateForm(Product product) {
        if (product == null) return;
        
        nameField.setText(product.getName() != null ? product.getName() : "");
        descriptionField.setText(product.getDescription() != null ? product.getDescription() : "");
        priceField.setText(String.valueOf(product.getPrice()));
        stockField.setText(String.valueOf(product.getStockQty()));
        skuField.setText(product.getSku() != null ? product.getSku() : "");

        // Sélectionner l'équipe dans le ComboBox
        String teamOption = product.getTeamId() + " - " + getTeamName(product.getTeamId());
        teamIdField.setValue(teamOption);

        activeCheckBox.setSelected(product.isActive());
    }

    /**
     * Récupérer le nom d'une team par son ID
     */
    private String getTeamName(int teamId) {
        for (Team team : teamList) {
            if (team.getTeamId() == teamId) {
                return team.getName();
            }
        }
        return "Équipe #" + teamId;
    }

    /**
     * Charger tous les produits
     */
    public void loadProducts() {
        try {
            List<Product> products = productService.getAll();
            productList = FXCollections.observableArrayList(products);
            productTable.setItems(productList);
            LOGGER.log(Level.INFO, "Produits chargés: " + products.size());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du chargement des produits: " + e.getMessage());
            showAlert("Erreur", "Impossible de charger les produits: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * Ajouter un nouveau produit
     */
    @FXML
    public void addProduct() {
        if (!validateForm()) {
            showAlert("Erreur de validation", "Veuillez remplir tous les champs correctement", Alert.AlertType.ERROR);
            return;
        }

        try {
            Product product = createProductFromForm();
            if (productService.add(product)) {
                int productId = getLastInsertedProductId();
                
                // Uploader l'image si une est sélectionnée
                if (selectedImageFile != null && productId > 0) {
                    int imageId = uploadProductImage(selectedImageFile);
                    if (imageId > 0) {
                        imageService.addImageToProduct(productId, imageId, 1);
                        LOGGER.log(Level.INFO, "Image associée au produit");
                    }
                }
                
                showAlert("Succès", "Produit créé avec succès!", Alert.AlertType.INFORMATION);
                resetForm();
                loadProducts();
                selectedImageFile = null;
            } else {
                showAlert("Erreur", "Impossible de créer le produit", Alert.AlertType.ERROR);
            }
        } catch (NumberFormatException e) {
            showAlert("Erreur", "Erreur de format des données: " + e.getMessage(), Alert.AlertType.ERROR);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur SQL lors de l'ajout du produit: " + e.getMessage());
            showAlert("Erreur", "Erreur de base de données: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * Récupérer l'ID du dernier produit inséré
     */
    private int getLastInsertedProductId() {
        try {
            List<Product> products = productService.getAll();
            if (!products.isEmpty()) {
                return products.get(0).getProductId();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Erreur lors de la récupération du dernier produit: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Mettre à jour le produit sélectionné
     */
    @FXML
    public void updateProduct() {
        if (selectedProduct == null) {
            showAlert("Attention", "Veuillez sélectionner un produit à modifier", Alert.AlertType.WARNING);
            return;
        }

        if (!validateForm()) {
            showAlert("Erreur de validation", "Veuillez remplir tous les champs correctement", Alert.AlertType.ERROR);
            return;
        }

        try {
            Product product = createProductFromForm();
            product.setProductId(selectedProduct.getProductId());
            
            if (productService.update(product)) {
                showAlert("Succès", "Produit mis à jour avec succès!", Alert.AlertType.INFORMATION);
                resetForm();
                loadProducts();
            } else {
                showAlert("Erreur", "Impossible de mettre à jour le produit", Alert.AlertType.ERROR);
            }
        } catch (NumberFormatException e) {
            showAlert("Erreur", "Erreur de format des données: " + e.getMessage(), Alert.AlertType.ERROR);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur SQL lors de la mise à jour du produit: " + e.getMessage());
            showAlert("Erreur", "Erreur de base de données: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * Supprimer le produit sélectionné avec confirmation
     */
    @FXML
    public void deleteProduct() {
        if (selectedProduct == null) {
            showAlert("Attention", "Veuillez sélectionner un produit à supprimer", Alert.AlertType.WARNING);
            return;
        }

        // Demander une confirmation
        Optional<ButtonType> result = showConfirmation(
            "Confirmation",
            "Êtes-vous sûr de vouloir supprimer '" + selectedProduct.getName() + "'?\nCette action est irréversible."
        );

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                if (productService.delete(selectedProduct.getProductId())) {
                    showAlert("Succès", "Produit supprimé avec succès!", Alert.AlertType.INFORMATION);
                    resetForm();
                    loadProducts();
                } else {
                    showAlert("Erreur", "Impossible de supprimer le produit", Alert.AlertType.ERROR);
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Erreur SQL lors de la suppression du produit: " + e.getMessage());
                showAlert("Erreur", "Erreur de base de données: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    /**
     * Réinitialiser le formulaire
     */
    @FXML
    public void resetForm() {
        nameField.clear();
        descriptionField.clear();
        priceField.clear();
        stockField.clear();
        skuField.clear();
        teamIdField.setValue(null);
        activeCheckBox.setSelected(true);
        selectedProduct = null;
        selectedImageFile = null;
        productTable.getSelectionModel().clearSelection();
        initializeImagePreview();
        updateDescriptionCounter(); // Reset counter to 0
    }

    /**
     * Sélectionner une image pour le produit
     */
    @FXML
    public void selectImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner une image pour le produit");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Images (*.jpg, *.jpeg, *.png, *.gif)", "*.jpg", "*.jpeg", "*.png", "*.gif"),
            new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
        );
        
        Stage stage = (Stage) selectImageButton.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
        
        if (file != null) {
            if (file.length() > MAX_IMAGE_SIZE) {
                showAlert("Erreur", "L'image est trop grande. Taille max: 5 MB", Alert.AlertType.ERROR);
                return;
            }
            
            selectedImageFile = file;
            if (imageNameLabel != null) {
                imageNameLabel.setText(file.getName());
            }
            
            // Afficher l'aperçu
            try {
                Image image = new Image(file.toURI().toString());
                if (imagePreview != null) {
                    imagePreview.setImage(image);
                }
                LOGGER.log(Level.INFO, "Image sélectionnée: " + file.getName());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Erreur lors du chargement de l'aperçu: " + e.getMessage());
            }
        }
    }

    /**
     * Uploader l'image sélectionnée et retourner l'ID de l'image
     */
    private int uploadProductImage(File imageFile) throws SQLException {
        if (imageFile == null) {
            return -1;
        }
        
        try {
            // Créer le répertoire d'upload s'il n'existe pas
            File uploadDir = new File("uploads/products/");
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }
            
            // Copier le fichier dans le répertoire d'upload
            String fileName = System.currentTimeMillis() + "_" + imageFile.getName();
            File destFile = new File(uploadDir, fileName);
            
            java.nio.file.Files.copy(imageFile.toPath(), destFile.toPath(), 
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            
            String fileUrl = "uploads/products/" + fileName;
            String mimeType = getMimeType(imageFile.getName());
            long fileSize = destFile.length();
            
            // Créer l'enregistrement dans la base de données
            int imageId = imageService.createImage(fileUrl, mimeType, fileSize, null, null, 
                imageFile.getName(), null);
            
            if (imageId > 0) {
                LOGGER.log(Level.INFO, "Image uploadée avec succès. ID: " + imageId);
                return imageId;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'upload de l'image: " + e.getMessage());
            throw new SQLException("Erreur lors de l'upload de l'image: " + e.getMessage());
        }
        
        return -1;
    }

    /**
     * Déterminer le type MIME en fonction de l'extension
     */
    private String getMimeType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }

    /**
     * Créer un objet Product à partir du formulaire
     */
    private Product createProductFromForm() throws NumberFormatException {
        Product product = new Product();
        product.setName(nameField.getText().trim());
        product.setDescription(descriptionField.getText().trim());
        product.setPrice(Double.parseDouble(priceField.getText().trim()));
        product.setStockQty(Integer.parseInt(stockField.getText().trim()));
        product.setSku(skuField.getText().trim());
        
        // Extraire l'ID de la team depuis le ComboBox (format "ID - Nom")
        String teamSelection = teamIdField.getValue();
        if (teamSelection != null && !teamSelection.isEmpty()) {
            int teamId = Integer.parseInt(teamSelection.split(" - ")[0].trim());
            product.setTeamId(teamId);
        } else {
            throw new NumberFormatException("Veuillez sélectionner une équipe");
        }
        
        product.setActive(activeCheckBox.isSelected());
        return product;
    }

    /**
     * Valider les données du formulaire
     */
    private boolean validateForm() {
        try {
            // Nom
            String name = nameField.getText().trim();
            if (name.isEmpty() || name.length() > NAME_MAX_LENGTH) {
                showFieldError("nameField", "Le nom doit faire entre 1 et " + NAME_MAX_LENGTH + " caractères");
                return false;
            }

            // Description
            String description = descriptionField.getText().trim();
            if (description.isEmpty()) {
                showFieldError("descriptionField", "La description est obligatoire");
                return false;
            }

            // Prix
            double price = Double.parseDouble(priceField.getText().trim());
            if (price < MIN_PRICE || price > MAX_PRICE) {
                showFieldError("priceField", "Le prix doit être entre " + MIN_PRICE + " et " + MAX_PRICE);
                return false;
            }

            // Stock
            int stock = Integer.parseInt(stockField.getText().trim());
            if (stock < 0 || stock > MAX_STOCK) {
                showFieldError("stockField", "Le stock doit être entre 0 et " + MAX_STOCK);
                return false;
            }

            // SKU
            String sku = skuField.getText().trim();
            if (sku.isEmpty() || sku.length() > SKU_MAX_LENGTH) {
                showFieldError("skuField", "Le SKU doit faire entre 1 et " + SKU_MAX_LENGTH + " caractères");
                return false;
            }

            // Team ID (depuis le ComboBox)
            String teamSelection = teamIdField.getValue();
            if (teamSelection == null || teamSelection.isEmpty()) {
                showFieldError("teamIdField", "Veuillez sélectionner une équipe");
                return false;
            }

            int teamId = Integer.parseInt(teamSelection.split(" - ")[0].trim());
            if (teamId <= 0) {
                showFieldError("teamIdField", "L'ID de la team doit être positif");
                return false;
            }

            // Vérifier que la team existe
            if (!teamService.exists(teamId)) {
                showFieldError("teamIdField", "L'équipe sélectionnée n'existe pas");
                return false;
            }

            return true;
        } catch (NumberFormatException e) {
            showAlert("Erreur", "Format de nombre invalide: " + e.getMessage(), Alert.AlertType.ERROR);
            return false;
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors de la vérification de la team: " + e.getMessage(), Alert.AlertType.ERROR);
            return false;
        }
    }

    /**
     * Marquer un champ comme ayant une erreur
     */
    private void showFieldError(String fieldId, String message) {
        showAlert("Erreur de validation - " + fieldId, message, Alert.AlertType.ERROR);
    }

    /**
     * Naviguer vers une autre page
     */
    @FXML private void goToOrders() { navigateTo("/fxml/Admin/OrderManagement.fxml"); }
    @FXML private void goBack() { navigateTo("/fxml/Front/ListProducts.fxml"); }

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

            Stage stage = (Stage) productTable.getScene().getWindow();
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

    /**
     * Afficher une confirmation
     */
    private Optional<ButtonType> showConfirmation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait();
    }
}




