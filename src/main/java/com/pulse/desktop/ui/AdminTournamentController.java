package com.pulse.desktop.ui;

import com.pulse.desktop.model.LookupItem;
import com.pulse.desktop.model.RouteDefinition;
import com.pulse.desktop.model.TournamentModel;
import com.pulse.desktop.repo.LookupRepository;
import com.pulse.desktop.repo.TournamentRepository;
import com.pulse.desktop.util.AlertUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.math.BigDecimal;
import java.time.LocalDate;

public class AdminTournamentController implements RouteAwarePage {

    @FXML private TableView<TournamentModel> tournamentsTable;
    @FXML private TableColumn<TournamentModel, Integer> idCol;
    @FXML private TableColumn<TournamentModel, String> titleCol;
    @FXML private TableColumn<TournamentModel, String> organizerCol;
    @FXML private TableColumn<TournamentModel, String> gameCol;
    @FXML private TableColumn<TournamentModel, LocalDate> startCol;
    @FXML private TableColumn<TournamentModel, LocalDate> endCol;
    @FXML private TableColumn<TournamentModel, Integer> maxTeamsCol;
    @FXML private TableColumn<TournamentModel, String> formatCol;
    @FXML private TableColumn<TournamentModel, String> statusCol;
    @FXML private TableColumn<TournamentModel, BigDecimal> prizeCol;

    @FXML private ComboBox<LookupItem> organizerBox;
    @FXML private ComboBox<LookupItem> gameBox;
    @FXML private TextField titleField;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private TextField maxTeamsField;
    @FXML private TextField formatField;
    @FXML private TextField statusField;
    @FXML private TextField prizePoolField;

    private final TournamentRepository tournamentRepo = new TournamentRepository();
    private final LookupRepository lookupRepo = new LookupRepository();

    private final ObservableList<TournamentModel> data = FXCollections.observableArrayList();
    private final ObservableList<LookupItem> organizers = FXCollections.observableArrayList();
    private final ObservableList<LookupItem> games = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        tournamentsTable.setItems(data);
        organizerBox.setItems(organizers);
        gameBox.setItems(games);

        tournamentsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel == null) {
                return;
            }
            titleField.setText(newSel.getTitle());
            startDatePicker.setValue(newSel.getStartDate());
            endDatePicker.setValue(newSel.getEndDate());
            maxTeamsField.setText(Integer.toString(newSel.getMaxTeams()));
            formatField.setText(newSel.getFormat());
            statusField.setText(newSel.getStatus());
            prizePoolField.setText(newSel.getPrizePool() == null ? "" : newSel.getPrizePool().toPlainString());

            if (newSel.getOrganizerUserId() != null) {
                organizers.stream()
                        .filter(item -> item.getId() == newSel.getOrganizerUserId())
                        .findFirst()
                        .ifPresent(item -> organizerBox.getSelectionModel().select(item));
            }
            if (newSel.getGameId() != null) {
                games.stream()
                        .filter(item -> item.getId() == newSel.getGameId())
                        .findFirst()
                        .ifPresent(item -> gameBox.getSelectionModel().select(item));
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
            organizers.setAll(lookupRepo.users());
            games.setAll(lookupRepo.games());
            data.setAll(tournamentRepo.findAll());
        } catch (Exception e) {
            AlertUtils.error("Erreur DB", "Chargement echoue: " + e.getMessage());
        }
    }

    @FXML
    private void handleAdd() {
        try {
            LookupItem organizer = organizerBox.getSelectionModel().getSelectedItem();
            LookupItem game = gameBox.getSelectionModel().getSelectedItem();
            if (organizer == null || game == null) {
                AlertUtils.warning("Validation", "Veuillez choisir un organisateur et un jeu.");
                return;
            }
            if (titleField.getText() == null || titleField.getText().isBlank()) {
                AlertUtils.warning("Validation", "Veuillez saisir un titre.");
                return;
            }
            if (startDatePicker.getValue() == null || endDatePicker.getValue() == null) {
                AlertUtils.warning("Validation", "Veuillez saisir les dates.");
                return;
            }

            TournamentModel tournament = new TournamentModel();
            tournament.setOrganizerUserId(organizer.getId());
            tournament.setGameId(game.getId());
            tournament.setTitle(titleField.getText().trim());
            tournament.setStartDate(startDatePicker.getValue());
            tournament.setEndDate(endDatePicker.getValue());
            tournament.setMaxTeams(parseIntOrDefault(maxTeamsField.getText(), 16));
            tournament.setFormat(textOrDefault(formatField.getText(), "BO1"));
            tournament.setStatus(textOrDefault(statusField.getText(), "DRAFT"));
            tournament.setPrizePool(parseMoneyOrDefault(prizePoolField.getText(), BigDecimal.ZERO));

            tournamentRepo.insert(tournament);
            AlertUtils.info("Succes", "Tournoi ajoute.");
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
        TournamentModel sel = tournamentsTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AlertUtils.warning("Attention", "Veuillez selectionner un tournoi.");
            return;
        }
        try {
            LookupItem organizer = organizerBox.getSelectionModel().getSelectedItem();
            LookupItem game = gameBox.getSelectionModel().getSelectedItem();
            if (organizer == null || game == null) {
                AlertUtils.warning("Validation", "Veuillez choisir un organisateur et un jeu.");
                return;
            }
            if (titleField.getText() == null || titleField.getText().isBlank()) {
                AlertUtils.warning("Validation", "Veuillez saisir un titre.");
                return;
            }
            if (startDatePicker.getValue() == null || endDatePicker.getValue() == null) {
                AlertUtils.warning("Validation", "Veuillez saisir les dates.");
                return;
            }

            sel.setOrganizerUserId(organizer.getId());
            sel.setGameId(game.getId());
            sel.setTitle(titleField.getText().trim());
            sel.setStartDate(startDatePicker.getValue());
            sel.setEndDate(endDatePicker.getValue());
            sel.setMaxTeams(parseIntOrDefault(maxTeamsField.getText(), sel.getMaxTeams()));
            sel.setFormat(textOrDefault(formatField.getText(), sel.getFormat()));
            sel.setStatus(textOrDefault(statusField.getText(), sel.getStatus()));
            BigDecimal fallback = sel.getPrizePool() == null ? BigDecimal.ZERO : sel.getPrizePool();
            sel.setPrizePool(parseMoneyOrDefault(prizePoolField.getText(), fallback));

            tournamentRepo.update(sel);
            AlertUtils.info("Succes", "Tournoi modifie.");
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
        TournamentModel sel = tournamentsTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AlertUtils.warning("Attention", "Veuillez selectionner un tournoi.");
            return;
        }
        try {
            tournamentRepo.deleteById(sel.getTournamentId());
            AlertUtils.info("Succes", "Tournoi supprime.");
            clearFields();
            loadData();
        } catch (Exception e) {
            AlertUtils.error("Erreur", "Suppression echouee: " + e.getMessage());
        }
    }

    @FXML
    private void clearFields() {
        tournamentsTable.getSelectionModel().clearSelection();
        organizerBox.getSelectionModel().clearSelection();
        gameBox.getSelectionModel().clearSelection();
        titleField.clear();
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
        maxTeamsField.clear();
        formatField.clear();
        statusField.clear();
        prizePoolField.clear();
    }

    private static int parseIntOrDefault(String raw, int fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Max equipes invalide: " + raw);
        }
    }

    private static BigDecimal parseMoneyOrDefault(String raw, BigDecimal fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Prize pool invalide: " + raw);
        }
    }

    private static String textOrDefault(String raw, String fallback) {
        if (raw == null) {
            return fallback;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }
}
