package com.pulse.desktop.ui;

import com.pulse.desktop.model.GameModel;
import com.pulse.desktop.model.RouteDefinition;
import com.pulse.desktop.repo.GameRepository;
import com.pulse.desktop.service.Navigator;
import com.pulse.desktop.service.RouteContext;
import com.pulse.desktop.util.AlertUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.time.format.DateTimeFormatter;

public class FrontGameDetailController implements RouteAwarePage {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML private Label routeNameLabel;
    @FXML private Label routeIdLabel;
    @FXML private Label routeDescriptionLabel;

    @FXML private Label gameTitleLabel;
    @FXML private Label categoryLabel;
    @FXML private Label publisherLabel;
    @FXML private Label statusLabel;
    @FXML private Label popularityLabel;
    @FXML private Label viewsLabel;
    @FXML private Label favoritesLabel;
    @FXML private Label createdAtLabel;
    @FXML private Label descriptionLabel;

    private final GameRepository gameRepo = new GameRepository();

    @FXML
    private void goHome() {
        Navigator.goTo("front_home");
    }

    @FXML
    private void goBackToGames() {
        Navigator.goTo("front_games");
    }

    @Override
    public void setRoute(RouteDefinition routeDefinition) {
        if (routeNameLabel != null) {
            routeNameLabel.setText("Detail jeu");
        }
        if (routeIdLabel != null) {
            routeIdLabel.setText("Route Symfony: " + routeDefinition.id());
        }
        if (routeDescriptionLabel != null) {
            routeDescriptionLabel.setText("Detail du jeu selectionne (depuis le catalogue).");
        }

        loadSelectedGame();
    }

    private void loadSelectedGame() {
        Integer gameId = RouteContext.getSelectedGameId();
        if (gameId == null) {
            renderMissingSelection();
            return;
        }

        try {
            GameModel game = gameRepo.findById(gameId);
            if (game == null) {
                renderMissingSelection();
                return;
            }
            renderGame(game);
        } catch (Exception e) {
            AlertUtils.error("Erreur DB", "Chargement jeu echoue: " + e.getMessage());
            renderMissingSelection();
        }
    }

    private void renderGame(GameModel game) {
        gameTitleLabel.setText(safe(game.getName()));
        categoryLabel.setText(safe(game.getCategoryName()));
        publisherLabel.setText(safe(game.getPublisher()));
        statusLabel.setText(safe(game.getStatus()));
        popularityLabel.setText(Integer.toString(game.getPopularityScore()));
        viewsLabel.setText(Integer.toString(game.getViewsCount()));
        favoritesLabel.setText(Integer.toString(game.getFavoritesCount()));
        createdAtLabel.setText(game.getCreatedAt() == null ? "-" : game.getCreatedAt().format(DATE_TIME));
        descriptionLabel.setText(safe(game.getDescription()));

        if (routeNameLabel != null) {
            routeNameLabel.setText("Detail: " + safe(game.getName()));
        }
        if (routeDescriptionLabel != null) {
            routeDescriptionLabel.setText("Categorie: " + safe(game.getCategoryName()) + " | Editeur: " + safe(game.getPublisher()));
        }
    }

    private void renderMissingSelection() {
        gameTitleLabel.setText("Aucun jeu selectionne");
        categoryLabel.setText("-");
        publisherLabel.setText("-");
        statusLabel.setText("-");
        popularityLabel.setText("-");
        viewsLabel.setText("-");
        favoritesLabel.setText("-");
        createdAtLabel.setText("-");
        descriptionLabel.setText("Retournez au catalogue et choisissez un jeu.");
    }

    private static String safe(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value;
    }
}

