package com.pulse.desktop.ui;

import com.pulse.desktop.model.RouteDefinition;
import com.pulse.desktop.repo.TeamModuleRepository;
import com.pulse.desktop.service.Navigator;
import com.pulse.desktop.service.RouteContext;
import com.pulse.desktop.util.AlertUtils;
import com.pulse.desktop.util.ImageResolver;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FrontTeamsController implements RouteAwarePage {
    @FXML
    private TextField qField;
    @FXML
    private ComboBox<String> regionCombo;
    @FXML
    private ComboBox<String> sortCombo;
    @FXML
    private CheckBox withProductsCheck;
    @FXML
    private CheckBox activeTournamentsCheck;
    @FXML
    private Label resultCountLabel;
    @FXML
    private FlowPane teamsPane;

    private final TeamModuleRepository repository = new TeamModuleRepository();
    private List<TeamModuleRepository.TeamCatalogRow> currentRows = List.of();

    @FXML
    public void initialize() {
        sortCombo.setItems(FXCollections.observableArrayList("latest", "oldest", "name", "region", "popular"));
        sortCombo.getSelectionModel().select("latest");
    }

    @Override
    public void setRoute(RouteDefinition routeDefinition) {
        loadRegions();
        refresh();
    }

    @FXML
    private void applyFilters() {
        refresh();
    }

    @FXML
    private void resetFilters() {
        qField.clear();
        if (!regionCombo.getItems().isEmpty()) {
            regionCombo.getSelectionModel().select(0);
        }
        sortCombo.getSelectionModel().select("latest");
        withProductsCheck.setSelected(false);
        activeTournamentsCheck.setSelected(false);
        refresh();
    }

    @FXML
    private void openTournaments() {
        Navigator.goTo("front_tournaments");
    }

    private void loadRegions() {
        try {
            List<String> items = new ArrayList<>();
            items.add("");
            items.addAll(repository.listRegions());
            regionCombo.setItems(FXCollections.observableArrayList(items));
            if (regionCombo.getValue() == null) {
                regionCombo.getSelectionModel().select(0);
            }
        } catch (SQLException ex) {
            AlertUtils.error("Equipes", "Impossible de charger les regions.\n" + ex.getMessage());
        }
    }

    private void refresh() {
        try {
            TeamModuleRepository.TeamCatalogFilter filter = new TeamModuleRepository.TeamCatalogFilter(
                    qField.getText(),
                    regionCombo.getValue(),
                    sortCombo.getValue(),
                    withProductsCheck.isSelected(),
                    activeTournamentsCheck.isSelected()
            );
            currentRows = repository.searchTeamsCatalog(filter, 300);
            resultCountLabel.setText(currentRows.size() + " equipe(s)");
            renderRows();
        } catch (SQLException ex) {
            AlertUtils.error("Equipes", "Erreur de chargement du catalogue.\n" + ex.getMessage());
        }
    }

    private void renderRows() {
        teamsPane.getChildren().clear();
        if (currentRows.isEmpty()) {
            teamsPane.getChildren().add(CompetitionUi.emptyState("Aucune equipe ne correspond aux filtres."));
            return;
        }

        for (TeamModuleRepository.TeamCatalogRow row : currentRows) {
            teamsPane.getChildren().add(buildCard(row));
        }
    }

    private VBox buildCard(TeamModuleRepository.TeamCatalogRow row) {
        String fallback = "https://picsum.photos/seed/pulse_team_" + row.teamId() + "/1200/800";
        String imageUrl = ImageResolver.toExternalForm(row.logoPath());
        if (imageUrl == null || imageUrl.isBlank()) {
            imageUrl = fallback;
        }

        ImageView cover = new ImageView(new Image(imageUrl, true));
        cover.setPreserveRatio(false);
        cover.setFitWidth(340);
        cover.setFitHeight(142);
        cover.getStyleClass().add("team-card-cover");

        StackPane media = new StackPane(cover);
        media.getStyleClass().add("card__media");
        media.setPrefHeight(142);
        media.setMaxWidth(Double.MAX_VALUE);

        Label title = new Label(CompetitionUi.emptySafe(row.name()));
        title.getStyleClass().add("card__title");

        Label desc = new Label(shortDescription(row.description()));
        desc.setWrapText(true);
        desc.getStyleClass().add("card__desc");

        HBox chips = new HBox(6,
                chip("Region: " + CompetitionUi.emptySafe(row.region())),
                chip("Membres: " + row.membersCount()),
                chip("Produits: " + row.productsCount()),
                chip("Tournois: " + row.activeTournamentsCount())
        );
        chips.setAlignment(Pos.CENTER_LEFT);

        Label captain = new Label("Capitaine: " + CompetitionUi.emptySafe(row.captainName()));
        captain.getStyleClass().add("list-item-meta");

        Button detailButton = new Button("Detail equipe");
        detailButton.getStyleClass().add("btn-ghost");
        detailButton.setOnAction(event -> {
            RouteContext.putInt(RouteContext.KEY_TEAM_ID, row.teamId());
            Navigator.goTo("front_team_detail");
        });

        Button shopButton = new Button("Boutique equipe");
        shopButton.getStyleClass().add("btn-primary");
        shopButton.setOnAction(event -> {
            RouteContext.putInt(RouteContext.KEY_TEAM_ID, row.teamId());
            Navigator.goTo("front_shop");
        });

        Region actionsSpacer = new Region();
        HBox.setHgrow(actionsSpacer, Priority.ALWAYS);
        HBox actions = new HBox(8, detailButton, actionsSpacer, shopButton);
        actions.getStyleClass().add("card__actions");

        VBox body = new VBox(8, title, desc, chips, captain, actions);
        body.getStyleClass().add("card__body");

        VBox card = new VBox(media, body);
        card.getStyleClass().addAll("card", "team-card");
        card.setPrefWidth(350);
        card.setMaxWidth(350);
        return card;
    }

    private static Label chip(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("chip");
        return label;
    }

    private static String shortDescription(String description) {
        String value = description == null ? "" : description.trim();
        if (value.isBlank()) {
            return "Equipe e-sport active sur PULSE.";
        }
        if (value.length() <= 140) {
            return value;
        }
        return value.substring(0, 137).trim() + "...";
    }
}
