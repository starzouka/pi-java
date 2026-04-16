package org.example.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.model.Order;
import org.example.service.OrderService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class OrderManagementController {
    @FXML
    private TableView<Order> orderTable;
    @FXML
    private ComboBox<String> statusFilter;
    @FXML
    private BarChart<String, Number> statsChart;

    private OrderService orderService;
    private ObservableList<Order> orderList;

    @FXML
    public void initialize() {
        orderService = new OrderService();
        setupStatusFilter();
        loadOrders();
        loadStatistics();
    }

    private void setupStatusFilter() {
        statusFilter.setItems(FXCollections.observableArrayList(
                "Tous", "PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED"
        ));
        statusFilter.setValue("Tous");
        statusFilter.setOnAction(e -> filterOrders());
    }

    @FXML
    public void loadOrders() {
        try {
            List<Order> orders = orderService.getAll();
            orderList = FXCollections.observableArrayList(orders);
            filterOrders();
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les commandes: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void filterOrders() {
        String selectedStatus = statusFilter.getValue();
        ObservableList<Order> filteredList = FXCollections.observableArrayList();

        if (selectedStatus.equals("Tous")) {
            filteredList.addAll(orderList);
        } else {
            for (Order order : orderList) {
                if (order.getStatus().equals(selectedStatus)) {
                    filteredList.add(order);
                }
            }
        }
        orderTable.setItems(filteredList);
    }

    private void loadStatistics() {
        try {
            List<Object[]> stats = orderService.getStatsByStatus();

            CategoryAxis xAxis = new CategoryAxis();
            NumberAxis yAxis = new NumberAxis();
            yAxis.setLabel("Nombre de commandes");

            BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
            chart.setTitle("Commandes par Statut");

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Commandes");

            for (Object[] stat : stats) {
                String status = (String) stat[0];
                int count = (Integer) stat[1];
                series.getData().add(new XYChart.Data<>(status, count));
            }

            statsChart.getData().add(series);
        } catch (SQLException e) {
            System.err.println("Impossible de charger les statistiques: " + e.getMessage());
        }
    }

     @FXML
     private void viewDetails() {
         Order selectedOrder = orderTable.getSelectionModel().getSelectedItem();
         if (selectedOrder == null) {
             showAlert("Attention", "Veuillez sélectionner une commande", Alert.AlertType.WARNING);
             return;
         }

         try {
             String details = "Numéro: " + selectedOrder.getOrderNumber() +
                     "\nUtilisateur ID: " + selectedOrder.getUserId() +
                     "\nDate: " + selectedOrder.getCreatedAt() +
                     "\nTotal: " + selectedOrder.getTotalAmount() + " €" +
                     "\nStatut: " + selectedOrder.getStatus() +
                     "\nPaiement: " + selectedOrder.getPaymentStatus() +
                     "\nAdresse: " + selectedOrder.getShippingAddress();

             Alert alert = new Alert(Alert.AlertType.INFORMATION);
             alert.setTitle("Détails de la commande");
             alert.setHeaderText("Commande #" + selectedOrder.getOrderNumber());
             alert.setContentText(details);
             alert.showAndWait();
         } catch (Exception e) {
             showAlert("Erreur", "Impossible d'afficher les détails", Alert.AlertType.ERROR);
         }
     }

     @FXML
     private void updateStatus() {
         Order selectedOrder = orderTable.getSelectionModel().getSelectedItem();
         if (selectedOrder == null) {
             showAlert("Attention", "Veuillez sélectionner une commande", Alert.AlertType.WARNING);
             return;
         }

         ChoiceDialog<String> dialog = new ChoiceDialog<>("PENDING",
                 FXCollections.observableArrayList("PENDING", "PAID", "CANCELLED", "SHIPPED", "DELIVERED"));
         dialog.setTitle("Modifier le statut");
         dialog.setHeaderText("Nouveau statut");
         dialog.setContentText("Statut:");

         dialog.showAndWait().ifPresent(newStatus -> {
             try {
                 if (orderService.updateStatus(selectedOrder.getOrderId(), newStatus)) {
                     showAlert("Succès", "Statut mis à jour!", Alert.AlertType.INFORMATION);
                     loadOrders();
                     loadStatistics();
                 } else {
                     showAlert("Erreur", "Impossible de mettre à jour le statut", Alert.AlertType.ERROR);
                 }
             } catch (SQLException e) {
                 showAlert("Erreur", "Erreur de base de données: " + e.getMessage(), Alert.AlertType.ERROR);
             }
         });
     }

     @FXML
     private void deleteOrder() {
         Order selectedOrder = orderTable.getSelectionModel().getSelectedItem();
         if (selectedOrder == null) {
             showAlert("Attention", "Veuillez sélectionner une commande", Alert.AlertType.WARNING);
             return;
         }

         Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
         confirmAlert.setTitle("Confirmation");
         confirmAlert.setHeaderText("Supprimer la commande?");
         confirmAlert.setContentText("Êtes-vous sûr de vouloir supprimer la commande #" + selectedOrder.getOrderNumber() + "?");

         Optional<ButtonType> result = confirmAlert.showAndWait();
         if (result.isPresent() && result.get() == ButtonType.OK) {
             try {
                 if (orderService.delete(selectedOrder.getOrderId())) {
                     showAlert("Succès", "Commande supprimée!", Alert.AlertType.INFORMATION);
                     loadOrders();
                     loadStatistics();
                 } else {
                     showAlert("Erreur", "Impossible de supprimer la commande", Alert.AlertType.ERROR);
                 }
             } catch (SQLException e) {
                 showAlert("Erreur", "Erreur de base de données: " + e.getMessage(), Alert.AlertType.ERROR);
             }
         }
     }

    @FXML
    private void goToProducts() {
        navigateTo("CRUDProduct.fxml");
    }

    @FXML
    private void goBack() {
        navigateTo("../Front/ListProducts.fxml");
    }

    private void navigateTo(String fxmlFile) {
        try {
            FXMLLoader loader;

            if (fxmlFile.startsWith("../Front/")) {
                // Navigation vers Front
                loader = new FXMLLoader(getClass().getResource("/fxml/Front/" + fxmlFile.replace("../Front/", "")));
            } else {
                // Navigation vers Admin
                loader = new FXMLLoader(getClass().getResource("/fxml/Admin/" + fxmlFile));
            }

            Parent root = loader.load();
            Scene scene = new Scene(root, 1200, 700);

            // Apply modern CSS stylesheet
            String styleSheet = getClass().getResource("/css/style_javafx.css").toExternalForm();
            scene.getStylesheets().add(styleSheet);

            Stage stage = (Stage) orderTable.getScene().getWindow();
            stage.setScene(scene);
        } catch (IOException e) {
            showAlert("Erreur", "Impossible de charger la page: " + e.getMessage(), Alert.AlertType.ERROR);
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

