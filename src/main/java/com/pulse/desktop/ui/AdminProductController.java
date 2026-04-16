package com.pulse.desktop.ui;

import com.pulse.desktop.model.LookupItem;
import com.pulse.desktop.model.ProductModel;
import com.pulse.desktop.model.RouteDefinition;
import com.pulse.desktop.repo.LookupRepository;
import com.pulse.desktop.repo.ProductRepository;
import com.pulse.desktop.util.AlertUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.math.BigDecimal;

public class AdminProductController implements RouteAwarePage {

    @FXML private TableView<ProductModel> productsTable;
    @FXML private TableColumn<ProductModel, Integer> idCol;
    @FXML private TableColumn<ProductModel, String> teamCol;
    @FXML private TableColumn<ProductModel, String> nameCol;
    @FXML private TableColumn<ProductModel, BigDecimal> priceCol;
    @FXML private TableColumn<ProductModel, Integer> stockCol;
    @FXML private TableColumn<ProductModel, Boolean> activeCol;

    @FXML private ComboBox<LookupItem> teamBox;
    @FXML private TextField nameField;
    @FXML private TextField priceField;
    @FXML private TextField stockField;
    @FXML private CheckBox activeCheck;

    private final ProductRepository productRepo = new ProductRepository();
    private final LookupRepository lookupRepo = new LookupRepository();

    private final ObservableList<ProductModel> data = FXCollections.observableArrayList();
    private final ObservableList<LookupItem> teams = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        productsTable.setItems(data);
        teamBox.setItems(teams);

        productsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel == null) {
                return;
            }
            nameField.setText(newSel.getName());
            priceField.setText(newSel.getPrice() == null ? "" : newSel.getPrice().toPlainString());
            stockField.setText(Integer.toString(newSel.getStockQty()));
            activeCheck.setSelected(newSel.isActive());

            if (newSel.getTeamId() != null) {
                teams.stream()
                        .filter(item -> item.getId() == newSel.getTeamId())
                        .findFirst()
                        .ifPresent(item -> teamBox.getSelectionModel().select(item));
            }
        });

        loadData();
    }

    @Override
    public void setRoute(RouteDefinition routeDefinition) {
    }

    @FXML
    private void loadData() {
        try {
            teams.setAll(lookupRepo.teams());
            data.setAll(productRepo.findAll());
        } catch (Exception e) {
            AlertUtils.error("Erreur DB", "Chargement echoue: " + e.getMessage());
        }
    }

    @FXML
    private void handleAdd() {
        try {
            LookupItem team = teamBox.getSelectionModel().getSelectedItem();
            if (team == null) {
                AlertUtils.warning("Validation", "Veuillez choisir une equipe.");
                return;
            }
            if (nameField.getText() == null || nameField.getText().isBlank()) {
                AlertUtils.warning("Validation", "Veuillez saisir un nom de produit.");
                return;
            }

            ProductModel product = new ProductModel();
            product.setTeamId(team.getId());
            product.setName(nameField.getText().trim());
            product.setPrice(parseMoney(priceField.getText()));
            product.setStockQty(parseInt(stockField.getText()));
            product.setActive(activeCheck.isSelected());

            productRepo.insert(product);
            AlertUtils.info("Succes", "Produit ajoute.");
            clearFields();
            loadData();
        } catch (IllegalArgumentException e) {
            AlertUtils.warning("Validation", e.getMessage());
        } catch (Exception e) {
            AlertUtils.error("Erreur", "Ajout echoue: " + e.getMessage());
        }
    }

    @FXML
    private void handleUpdate() {
        ProductModel sel = productsTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AlertUtils.warning("Attention", "Veuillez selectionner un produit.");
            return;
        }
        try {
            LookupItem team = teamBox.getSelectionModel().getSelectedItem();
            if (team == null) {
                AlertUtils.warning("Validation", "Veuillez choisir une equipe.");
                return;
            }
            if (nameField.getText() == null || nameField.getText().isBlank()) {
                AlertUtils.warning("Validation", "Veuillez saisir un nom de produit.");
                return;
            }

            sel.setTeamId(team.getId());
            sel.setName(nameField.getText().trim());
            sel.setPrice(parseMoney(priceField.getText()));
            sel.setStockQty(parseInt(stockField.getText()));
            sel.setActive(activeCheck.isSelected());

            productRepo.update(sel);
            AlertUtils.info("Succes", "Produit modifie.");
            clearFields();
            loadData();
        } catch (IllegalArgumentException e) {
            AlertUtils.warning("Validation", e.getMessage());
        } catch (Exception e) {
            AlertUtils.error("Erreur", "Modification echouee: " + e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        ProductModel sel = productsTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AlertUtils.warning("Attention", "Veuillez selectionner un produit.");
            return;
        }
        try {
            productRepo.deleteById(sel.getProductId());
            AlertUtils.info("Succes", "Produit supprime.");
            clearFields();
            loadData();
        } catch (Exception e) {
            AlertUtils.error("Erreur", "Suppression echouee: " + e.getMessage());
        }
    }

    @FXML
    private void clearFields() {
        productsTable.getSelectionModel().clearSelection();
        teamBox.getSelectionModel().clearSelection();
        nameField.clear();
        priceField.clear();
        stockField.clear();
        activeCheck.setSelected(true);
    }

    private static int parseInt(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Stock invalide: vide");
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Stock invalide: " + raw);
        }
    }

    private static BigDecimal parseMoney(String raw) {
        if (raw == null || raw.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Prix invalide: " + raw);
        }
    }
}

