package com.pulse.desktop.ui;

import com.pulse.desktop.model.CategoryModel;
import com.pulse.desktop.model.RouteDefinition;
import com.pulse.desktop.repo.CategoryRepository;
import com.pulse.desktop.repo.GameRepository;
import com.pulse.desktop.util.AlertUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class AdminCategoryController implements RouteAwarePage {
    private static final String SORT_NEWEST = "Plus recents";
    private static final String SORT_NAME = "Nom A-Z";

    @FXML private FlowPane categoriesCardsPane;

    @FXML private TextField nameField;
    @FXML private TextField descField;
    @FXML private TextField slugField;

    @FXML private TextField qField;
    @FXML private ComboBox<String> sortBox;
    @FXML private Button filterButton;
    @FXML private Button resetButton;

    private final CategoryRepository categoryRepo = new CategoryRepository();
    private final GameRepository gameRepo = new GameRepository();
    private final ObservableList<CategoryModel> data = FXCollections.observableArrayList();
    private final ObservableList<String> sorts = FXCollections.observableArrayList();
    private final Map<Integer, VBox> cardById = new HashMap<>();

    private CategoryModel selectedCategory;

    @FXML
    public void initialize() {
        if (sortBox != null) {
            sortBox.setItems(sorts);
        }
        sorts.setAll(SORT_NEWEST, SORT_NAME);
        if (sortBox != null) {
            sortBox.getSelectionModel().select(SORT_NEWEST);
        }
        if (filterButton != null) {
            filterButton.setOnAction(event -> applyFilters());
        }
        if (resetButton != null) {
            resetButton.setOnAction(event -> resetFilters());
        }
        loadData();
    }

    @Override
    public void setRoute(RouteDefinition routeDefinition) {
    }

    @FXML
    private void loadData() {
        try {
            data.setAll(categoryRepo.findAll());
            applyFilters();
        } catch (Exception e) {
            AlertUtils.error("Erreur DB", "Chargement echoue: " + e.getMessage());
        }
    }

    @FXML
    private void applyFilters() {
        String q = normalize(qField == null ? "" : qField.getText());
        String sort = sortBox == null ? SORT_NEWEST : sortBox.getSelectionModel().getSelectedItem();

        List<CategoryModel> filtered = data.stream()
            .filter(category -> matchesQuery(category, q))
            .collect(Collectors.toList());

        sortCategories(filtered, sort);
        renderCards(filtered);
    }

    @FXML
    private void resetFilters() {
        if (qField != null) {
            qField.clear();
        }
        if (sortBox != null) {
            sortBox.getSelectionModel().select(SORT_NEWEST);
        }
        applyFilters();
    }

    @FXML
    private void handleAdd() {
        String name = safeText(nameField.getText());
        if (name.isBlank()) {
            AlertUtils.warning("Attention", "Le nom de la categorie est obligatoire.");
            return;
        }
        try {
            CategoryModel cat = new CategoryModel();
            cat.setName(name);
            cat.setDescription(descField.getText());
            cat.setSlug(resolveSlug(slugField.getText(), name));

            categoryRepo.insert(cat);
            AlertUtils.info("Succes", "Categorie ajoutee.");
            clearFields();
            loadData();
        } catch (Exception e) {
            AlertUtils.error("Erreur", "Ajout echoue: " + e.getMessage());
        }
    }

    @FXML
    private void handleUpdate() {
        if (selectedCategory == null) {
            AlertUtils.warning("Attention", "Veuillez selectionner une categorie.");
            return;
        }
        String name = safeText(nameField.getText());
        if (name.isBlank()) {
            AlertUtils.warning("Attention", "Le nom de la categorie est obligatoire.");
            return;
        }
        try {
            selectedCategory.setName(name);
            selectedCategory.setDescription(descField.getText());
            selectedCategory.setSlug(resolveSlug(slugField.getText(), name));

            categoryRepo.update(selectedCategory);
            AlertUtils.info("Succes", "Categorie modifiee.");
            clearFields();
            loadData();
        } catch (Exception e) {
            AlertUtils.error("Erreur", "Modification echouee: " + e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        if (selectedCategory == null) {
            AlertUtils.warning("Attention", "Veuillez selectionner une categorie.");
            return;
        }
        deleteCategory(selectedCategory);
    }

    @FXML
    private void clearFields() {
        clearSelected();
        nameField.clear();
        descField.clear();
        slugField.clear();
    }

    private void renderCards(List<CategoryModel> categories) {
        categoriesCardsPane.getChildren().clear();
        cardById.clear();

        if (categories.isEmpty()) {
            Label empty = new Label("Aucune categorie.");
            empty.getStyleClass().add("muted");
            categoriesCardsPane.getChildren().add(empty);
            return;
        }

        for (CategoryModel category : categories) {
            VBox card = buildCategoryCard(category);
            if (category.getCategoryId() != null) {
                cardById.put(category.getCategoryId(), card);
            }
            categoriesCardsPane.getChildren().add(card);
        }

        applySelectionStyle();
    }

    private VBox buildCategoryCard(CategoryModel category) {
        VBox cardBox = new VBox();
        cardBox.getStyleClass().addAll("card", "card--generic");
        cardBox.setPrefWidth(305);
        cardBox.setMinWidth(280);

        Region media = new Region();
        media.getStyleClass().add("card__media");
        media.setPrefHeight(112);

        HBox chips = new HBox(6);
        chips.getStyleClass().add("card__chips");

        Label idChip = new Label("ID " + safeId(category.getCategoryId()));
        idChip.getStyleClass().addAll("chip", "chip--category");
        Label slugChip = new Label(safe(category.getSlug()));
        slugChip.getStyleClass().addAll("chip", "chip--format");
        chips.getChildren().addAll(idChip, slugChip);

        StackPane mediaWrapper = new StackPane(media, chips);
        StackPane.setAlignment(chips, Pos.TOP_LEFT);

        VBox body = new VBox(8);
        body.getStyleClass().add("card__body");

        Label title = new Label(safe(category.getName()));
        title.getStyleClass().add("card__title");
        title.setWrapText(true);

        Label desc = new Label(safe(category.getDescription()));
        desc.getStyleClass().add("card__desc");
        desc.setWrapText(true);

        Button editButton = new Button("Editer");
        editButton.getStyleClass().addAll("btn", "btn--ghost", "btn--compact");
        editButton.setOnAction(event -> selectCategory(category));

        Button deleteButton = new Button("Supprimer");
        deleteButton.getStyleClass().addAll("btn", "btn--soft", "btn--compact");
        deleteButton.setOnAction(event -> deleteCategory(category));

        HBox actions = new HBox(8, editButton, deleteButton);
        actions.getStyleClass().add("card__actions");

        body.getChildren().addAll(title, desc, actions);
        cardBox.getChildren().addAll(mediaWrapper, body);

        cardBox.setOnMouseClicked(event -> selectCategory(category));
        return cardBox;
    }

    private void selectCategory(CategoryModel category) {
        selectedCategory = category;
        nameField.setText(category.getName());
        descField.setText(category.getDescription());
        slugField.setText(category.getSlug());
        applySelectionStyle();
    }

    private void deleteCategory(CategoryModel category) {
        if (category == null || category.getCategoryId() == null) {
            AlertUtils.warning("Attention", "Categorie invalide.");
            return;
        }

        try {
            int relatedGames = gameRepo.countByCategoryId(category.getCategoryId());
            if (relatedGames > 0) {
                Optional<CategoryModel> target = askReassignTarget(category, relatedGames);
                if (target.isEmpty()) {
                    return;
                }

                CategoryModel targetCategory = target.get();
                boolean ok = AlertUtils.confirm(
                    "Confirmer",
                    "Cette categorie contient " + relatedGames + " jeu(x).\n"
                        + "Deplacer vers \"" + safeText(targetCategory.getName()) + "\" puis supprimer ?"
                );
                if (!ok) {
                    return;
                }

                gameRepo.reassignCategory(category.getCategoryId(), targetCategory.getCategoryId());
            } else {
                boolean ok = AlertUtils.confirm("Confirmer", "Supprimer la categorie \"" + safeText(category.getName()) + "\" ?");
                if (!ok) {
                    return;
                }
            }
        } catch (Exception e) {
            AlertUtils.error("Erreur", "Verification relations echouee: " + e.getMessage());
            return;
        }

        try {
            categoryRepo.deleteById(category.getCategoryId());
            AlertUtils.info("Succes", "Categorie supprimee.");
            if (selectedCategory != null && category.getCategoryId().equals(selectedCategory.getCategoryId())) {
                clearFields();
            }
            loadData();
        } catch (Exception e) {
            AlertUtils.error("Erreur", "Suppression echouee: " + e.getMessage());
        }
    }

    private Optional<CategoryModel> askReassignTarget(CategoryModel toDelete, int relatedGames) {
        List<CategoryModel> options = data.stream()
            .filter(c -> c.getCategoryId() != null)
            .filter(c -> !c.getCategoryId().equals(toDelete.getCategoryId()))
            .collect(Collectors.toList());

        if (options.isEmpty()) {
            AlertUtils.warning(
                "Impossible",
                "Cette categorie contient " + relatedGames + " jeu(x) et aucune autre categorie n'existe.\n"
                    + "Creez une nouvelle categorie, puis reessayez."
            );
            return Optional.empty();
        }

        ChoiceDialog<CategoryModel> dialog = new ChoiceDialog<>(options.getFirst(), options);
        dialog.setTitle("Deplacer les jeux");
        dialog.setHeaderText("La categorie \"" + safeText(toDelete.getName()) + "\" est liee a " + relatedGames + " jeu(x).");
        dialog.setContentText("Deplacer vers :");
        return dialog.showAndWait();
    }

    private void clearSelected() {
        selectedCategory = null;
        applySelectionStyle();
    }

    private void applySelectionStyle() {
        for (VBox card : cardById.values()) {
            card.getStyleClass().remove("is-selected");
        }
        if (selectedCategory == null || selectedCategory.getCategoryId() == null) {
            return;
        }
        VBox card = cardById.get(selectedCategory.getCategoryId());
        if (card != null) {
            card.getStyleClass().add("is-selected");
        }
    }

    private static String safe(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value;
    }

    private static String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private static String safeId(Integer id) {
        return id == null ? "-" : id.toString();
    }

    private static String resolveSlug(String rawSlug, String rawName) {
        if (rawSlug != null && !rawSlug.isBlank()) {
            return rawSlug.trim();
        }
        if (rawName == null) {
            return "";
        }
        return rawName.trim().toLowerCase().replace(" ", "-");
    }

    private static void sortCategories(List<CategoryModel> categories, String sort) {
        if (sort == null || sort.isBlank() || SORT_NEWEST.equals(sort)) {
            categories.sort(Comparator
                .comparing(CategoryModel::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .reversed()
                .thenComparing(c -> safeText(c.getName())));
            return;
        }
        if (SORT_NAME.equals(sort)) {
            categories.sort(Comparator.comparing(c -> safeText(c.getName()).toLowerCase(Locale.ROOT)));
        }
    }

    private static boolean matchesQuery(CategoryModel category, String q) {
        if (q == null || q.isBlank()) {
            return true;
        }
        String name = normalize(category.getName());
        String slug = normalize(category.getSlug());
        String desc = normalize(category.getDescription());
        return name.contains(q) || slug.contains(q) || desc.contains(q);
    }

    private static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }
}
