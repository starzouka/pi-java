package com.pulse.desktop.ui;

import com.pulse.desktop.model.HomeHeroStats;
import com.pulse.desktop.model.RouteDefinition;
import com.pulse.desktop.model.SimpleCard;
import com.pulse.desktop.repo.HomeRepository;
import com.pulse.desktop.util.AlertUtils;
import com.pulse.desktop.util.ImageResolver;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;

import java.sql.SQLException;
import java.util.List;

public class HomeController implements RouteAwarePage {
    @FXML
    private TextField globalSearchField;
    @FXML
    private FlowPane searchResultsPane;
    @FXML
    private Label searchResultsTitle;
    @FXML
    private FlowPane weekTournamentsPane;
    @FXML
    private FlowPane bestSellersPane;
    @FXML
    private FlowPane popularGamesPane;
    @FXML
    private FlowPane topTeamsPane;
    @FXML
    private FlowPane latestMatchesPane;
    @FXML
    private Label heroMatchesValue;
    @FXML
    private Label heroTournamentsValue;
    @FXML
    private Label heroPlayersValue;
    @FXML
    private Region heroBackgroundRegion;

    private final HomeRepository repository = new HomeRepository();

    @FXML
    public void initialize() {
        String backgroundStyle = ImageResolver.toBackgroundStyle("assets/template_fo/img/ll.png");
        heroBackgroundRegion.setStyle(backgroundStyle);
        refreshAllSections();
    }

    @FXML
    private void refreshAllSections() {
        try {
            renderCards(weekTournamentsPane, repository.weekTournaments(8));
            renderCards(bestSellersPane, repository.bestProducts(8));
            renderCards(popularGamesPane, repository.popularGames(8));
            renderCards(topTeamsPane, repository.topTeams(8));
            renderCards(latestMatchesPane, repository.latestMatches(8));

            HomeHeroStats stats = repository.heroStats();
            heroMatchesValue.setText(Long.toString(stats.matches()));
            heroTournamentsValue.setText(Long.toString(stats.tournaments()));
            heroPlayersValue.setText(Long.toString(stats.players()));
        } catch (SQLException ex) {
            AlertUtils.error("Accueil", "Impossible de charger le contenu home.\n" + ex.getMessage());
        }
    }

    @FXML
    private void searchEverywhere() {
        String term = globalSearchField.getText();
        if (term == null || term.trim().length() < 2) {
            searchResultsTitle.setText("Resultats globaux: tapez au moins 2 caracteres.");
            searchResultsPane.getChildren().clear();
            return;
        }

        try {
            List<SimpleCard> cards = repository.globalSearch(term, 4);
            searchResultsTitle.setText("Resultats globaux pour: " + term);
            renderCards(searchResultsPane, cards);
        } catch (SQLException ex) {
            AlertUtils.error("Recherche", "Erreur lors de la recherche.\n" + ex.getMessage());
        }
    }

    @FXML
    private void clearSearch() {
        globalSearchField.clear();
        searchResultsPane.getChildren().clear();
        searchResultsTitle.setText("Resultats globaux (joueurs, equipes, tournois, jeux, produits)");
    }

    @Override
    public void setRoute(RouteDefinition routeDefinition) {
        // Home page does not need route parameters.
    }

    private void renderCards(FlowPane pane, List<SimpleCard> cards) {
        pane.getChildren().clear();
        if (cards.isEmpty()) {
            Label empty = new Label("Aucune donnee disponible.");
            empty.getStyleClass().add("muted");
            pane.getChildren().add(empty);
            return;
        }

        for (SimpleCard card : cards) {
            pane.getChildren().add(CardFactory.create(card));
        }
    }
}
