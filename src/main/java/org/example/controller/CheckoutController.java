package org.example.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.model.CartItem;
import org.example.model.Order;
import org.example.service.CartService;
import org.example.service.OrderService;
import org.example.util.SessionManager;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Pattern;

public class CheckoutController {
    @FXML
    private TableView<CartItem> orderSummaryTable;
    @FXML
    private Label totalLabel;
    @FXML
    private TextField emailField, phoneField;
    @FXML
    private TextArea addressField;
    @FXML
    private ComboBox<String> paymentMethodCombo;

    private CartService cartService;
    private OrderService orderService;
    private int currentUserId;
    private int currentCartId;
    private List<CartItem> cartItems;

    @FXML
    public void initialize() {
        cartService = new CartService();
        orderService = new OrderService();
        
        // Setup payment method combo
        if (paymentMethodCombo != null && paymentMethodCombo.getItems().isEmpty()) {
            paymentMethodCombo.setItems(FXCollections.observableArrayList("CARD", "CASH", "OTHER"));
            paymentMethodCombo.getSelectionModel().selectFirst();
        }
        
        // Récupérer les informations de session réelles
        currentUserId = SessionManager.getInstance().getCurrentUserId();
        try {
            currentCartId = cartService.getCartByUserId(currentUserId).getCartId();
        } catch (SQLException e) {
            currentCartId = 1; // Fallback
        }
        
        loadOrderSummary();
    }

    private void loadOrderSummary() {
        try {
            cartItems = cartService.getCartItems(currentCartId);
            ObservableList<CartItem> items = FXCollections.observableArrayList(cartItems);
            orderSummaryTable.setItems(items);
            updateTotal();
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger le résumé du panier: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void updateTotal() {
        try {
            double total = cartService.getCartTotal(currentCartId);
            totalLabel.setText(String.format("%.2f DT", total));
        } catch (SQLException e) {
            totalLabel.setText("0.00 DT");
        }
    }

    @FXML
    private void confirmOrder() {
        if (!validateForm()) return;

        try {
            double total = cartService.getCartTotal(currentCartId);
            
            // Récupérer la méthode de paiement sélectionnée
            String paymentMethod = "CARD";
            if (paymentMethodCombo != null && paymentMethodCombo.getValue() != null) {
                paymentMethod = paymentMethodCombo.getValue();
            }

            // Création de l'objet commande
            Order order = new Order("", currentCartId, currentUserId, total);

            String address = addressField.getText().trim();
            String phone = phoneField.getText().trim();

            // ✅ LOGGING DES VALEURS POUR DIAGNOSTIC
            System.out.println("===== DIAGNOSTIC CRÉATION COMMANDE =====");
            System.out.println("OrderNumber (généré par service): ''");
            System.out.println("CartId: " + currentCartId);
            System.out.println("UserId: " + currentUserId);
            System.out.println("TotalAmount: " + total);
            System.out.println("Status: 'PENDING' (longueur: " + "PENDING".length() + ")");
            System.out.println("PaymentMethod: '" + paymentMethod + "' (longueur: " + paymentMethod.length() + "/10)");
            System.out.println("PaymentStatus: 'UNPAID' (longueur: " + "UNPAID".length() + "/10)");
            System.out.println("ShippingAddress: '" + address + "' (longueur: " + address.length() + "/255)");
            System.out.println("PhoneForDelivery: '" + phone + "' (longueur: " + phone.length() + "/30)");
            System.out.println("=========================================");

            order.setShippingAddress(address);
            order.setPhoneForDelivery(phone);
            order.setPaymentMethod(paymentMethod);
            order.setStatus("PENDING");

            int orderId = orderService.add(order);
            if (orderId > 0) {
                // Actions post-commande
                // Mark cart as COMPLETED and create a new one for next purchase
                cartService.finalizeCartAfterOrder(currentCartId, currentUserId);

                showAlert("Succès", "🎮 Votre commande a été enregistrée avec succès !\nNuméro de commande : #" + orderId, Alert.AlertType.INFORMATION);
                navigateTo("ListProducts.fxml");
            } else {
                showAlert("Erreur", "Échec de la création de la commande. Veuillez réessayer.", Alert.AlertType.ERROR);
            }
        } catch (SQLException e) {
            System.out.println("❌ ERREUR SQL: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Erreur base de données : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private boolean validateForm() {
        String email = emailField.getText().trim();
        String address = addressField.getText().trim();
        String phone = phoneField.getText().trim();

        // Validation Email (Regex standard)
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        if (email.isEmpty() || !Pattern.matches(emailRegex, email)) {
            showAlert("Validation", "Veuillez entrer une adresse e-mail valide.", Alert.AlertType.WARNING);
            emailField.requestFocus();
            return false;
        }

        // Validation Téléphone (Chiffres uniquement, longueur min 8)
        if (phone.isEmpty() || !phone.matches("\\d+") || phone.length() < 8) {
            showAlert("Validation", "Veuillez entrer un numéro de téléphone valide (8 chiffres minimum).", Alert.AlertType.WARNING);
            phoneField.requestFocus();
            return false;
        }

        if (address.isEmpty() || address.length() < 10) {
            showAlert("Validation", "Veuillez entrer une adresse de livraison complète.", Alert.AlertType.WARNING);
            addressField.requestFocus();
            return false;
        }

        // Validation méthode de paiement
        if (paymentMethodCombo != null && paymentMethodCombo.getValue() == null) {
            showAlert("Validation", "Veuillez sélectionner une méthode de paiement.", Alert.AlertType.WARNING);
            paymentMethodCombo.requestFocus();
            return false;
        }

        if (cartItems == null || cartItems.isEmpty()) {
            showAlert("Panier", "Votre panier est vide.", Alert.AlertType.WARNING);
            return false;
        }

        return true;
    }

    @FXML
    private void goBack() {
        navigateTo("Cart.fxml");
    }

    private void navigateTo(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Front/" + fxmlFile));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1280, 720);

            // Charger le fichier de style centralisé
            String styleSheet = getClass().getResource("/css/style_front.css").toExternalForm();
            scene.getStylesheets().add(styleSheet);

            Stage stage = (Stage) orderSummaryTable.getScene().getWindow();
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            showAlert("Erreur Navigation", "Impossible de charger " + fxmlFile, Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        // Appliquer un style minimaliste à l'alerte si possible
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/css/style_front.css").toExternalForm());
        dialogPane.getStyleClass().add("card");
        
        alert.showAndWait();
    }
}
