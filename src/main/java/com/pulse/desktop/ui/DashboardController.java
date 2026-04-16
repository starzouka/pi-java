package com.pulse.desktop.ui;

import com.pulse.desktop.auth.SessionContext;
import com.pulse.desktop.model.DashboardStats;
import com.pulse.desktop.model.RouteDefinition;
import com.pulse.desktop.repo.DashboardRepository;
import com.pulse.desktop.service.Navigator;
import com.pulse.desktop.util.AlertUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.sql.SQLException;

public class DashboardController implements RouteAwarePage {
    @FXML
    private Label welcomeLabel;
    @FXML
    private Label usersLabel;
    @FXML
    private Label gamesLabel;
    @FXML
    private Label teamsLabel;
    @FXML
    private Label tournamentsLabel;
    @FXML
    private Label productsLabel;
    @FXML
    private Label ordersLabel;

    private final DashboardRepository repository = new DashboardRepository();

    @FXML
    public void initialize() {
        refresh();
    }

    @FXML
    private void refresh() {
        welcomeLabel.setText("Bienvenue " + SessionContext.currentDisplayName() + " (" + SessionContext.currentRole() + ")");
        try {
            DashboardStats stats = repository.loadStats();
            usersLabel.setText(Long.toString(stats.users()));
            gamesLabel.setText(Long.toString(stats.games()));
            teamsLabel.setText(Long.toString(stats.teams()));
            tournamentsLabel.setText(Long.toString(stats.tournaments()));
            productsLabel.setText(Long.toString(stats.products()));
            ordersLabel.setText(Long.toString(stats.orders()));
        } catch (SQLException ex) {
            AlertUtils.error("Dashboard", "Impossible de charger les stats.\n" + ex.getMessage());
        }
    }

    @FXML
    private void openMessages() {
        Navigator.goTo("front_messages");
    }

    @FXML
    private void openTeams() {
        Navigator.goTo("front_teams");
    }

    @FXML
    private void openTournaments() {
        Navigator.goTo("front_tournaments");
    }

    @FXML
    private void openShop() {
        Navigator.goTo("front_shop");
    }

    @Override
    public void setRoute(RouteDefinition routeDefinition) {
        // No route-specific state needed for this page.
    }
}
