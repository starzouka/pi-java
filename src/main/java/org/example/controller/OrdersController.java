package org.example.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.model.Order;
import org.example.service.OrderService;
import org.example.util.SessionManager;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class OrdersController {
    @FXML
    private TableView<Order> orderTable;

    private OrderService orderService;
    private ObservableList<Order> orderList;
    private int currentUserId;

    @FXML
    public void initialize() {
        orderService = new OrderService();
        currentUserId = SessionManager.getInstance().getCurrentUserId();
        loadOrders();
    }

    @FXML
    public void loadOrders() {
        try {
            List<Order> orders = orderService.getByUserId(currentUserId);
            orderList = FXCollections.observableArrayList(orders);
            orderTable.setItems(orderList);
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger vos commandes: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void goBack() {
        navigateTo("/fxml/Front/ListProducts.fxml");
    }

    private void navigateTo(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1280, 720);
            
            // Appliquer le style centralisé
            String styleSheet = getClass().getResource("/css/style_front.css").toExternalForm();
            scene.getStylesheets().add(styleSheet);

            Stage stage = (Stage) orderTable.getScene().getWindow();
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            showAlert("Erreur", "Impossible de charger " + fxmlPath, Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
