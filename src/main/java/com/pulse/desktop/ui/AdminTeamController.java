package com.pulse.desktop.ui;

import com.pulse.desktop.model.LookupItem;
import com.pulse.desktop.model.RouteDefinition;
import com.pulse.desktop.model.TeamModel;
import com.pulse.desktop.repo.LookupRepository;
import com.pulse.desktop.repo.TeamRepository;
import com.pulse.desktop.util.AlertUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.time.LocalDateTime;

public class AdminTeamController implements RouteAwarePage {

    @FXML private TableView<TeamModel> teamsTable;
    @FXML private TableColumn<TeamModel, Integer> idCol;
    @FXML private TableColumn<TeamModel, String> nameCol;
    @FXML private TableColumn<TeamModel, String> regionCol;
    @FXML private TableColumn<TeamModel, String> captainCol;
    @FXML private TableColumn<TeamModel, LocalDateTime> createdCol;

    @FXML private TextField nameField;
    @FXML private TextField regionField;
    @FXML private ComboBox<LookupItem> captainBox;

    private final TeamRepository teamRepo = new TeamRepository();
    private final LookupRepository lookupRepo = new LookupRepository();

    private final ObservableList<TeamModel> data = FXCollections.observableArrayList();
    private final ObservableList<LookupItem> captains = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        teamsTable.setItems(data);
        captainBox.setItems(captains);

        teamsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel == null) {
                return;
            }
            nameField.setText(newSel.getName());
            regionField.setText(newSel.getRegion());
            if (newSel.getCaptainUserId() != null) {
                captains.stream()
                        .filter(item -> item.getId() == newSel.getCaptainUserId())
                        .findFirst()
                        .ifPresent(item -> captainBox.getSelectionModel().select(item));
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
            captains.setAll(lookupRepo.users());
            data.setAll(teamRepo.findAll());
        } catch (Exception e) {
            AlertUtils.error("Erreur DB", "Chargement echoue: " + e.getMessage());
        }
    }

    @FXML
    private void handleAdd() {
        try {
            if (nameField.getText() == null || nameField.getText().isBlank()) {
                AlertUtils.warning("Validation", "Veuillez saisir un nom d'equipe.");
                return;
            }
            LookupItem captain = captainBox.getSelectionModel().getSelectedItem();
            if (captain == null) {
                AlertUtils.warning("Validation", "Veuillez choisir un capitaine.");
                return;
            }

            TeamModel team = new TeamModel();
            team.setName(nameField.getText().trim());
            team.setRegion(regionField.getText() == null ? "" : regionField.getText().trim());
            team.setCaptainUserId(captain.getId());

            teamRepo.insert(team);
            AlertUtils.info("Succes", "Equipe ajoutee.");
            clearFields();
            loadData();
        } catch (Exception e) {
            AlertUtils.error("Erreur", "Ajout echoue: " + e.getMessage());
        }
    }

    @FXML
    private void handleUpdate() {
        TeamModel sel = teamsTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AlertUtils.warning("Attention", "Veuillez selectionner une equipe.");
            return;
        }
        try {
            if (nameField.getText() == null || nameField.getText().isBlank()) {
                AlertUtils.warning("Validation", "Veuillez saisir un nom d'equipe.");
                return;
            }
            LookupItem captain = captainBox.getSelectionModel().getSelectedItem();
            if (captain == null) {
                AlertUtils.warning("Validation", "Veuillez choisir un capitaine.");
                return;
            }

            sel.setName(nameField.getText().trim());
            sel.setRegion(regionField.getText() == null ? "" : regionField.getText().trim());
            sel.setCaptainUserId(captain.getId());

            teamRepo.update(sel);
            AlertUtils.info("Succes", "Equipe modifiee.");
            clearFields();
            loadData();
        } catch (Exception e) {
            AlertUtils.error("Erreur", "Modification echouee: " + e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        TeamModel sel = teamsTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AlertUtils.warning("Attention", "Veuillez selectionner une equipe.");
            return;
        }
        try {
            teamRepo.deleteById(sel.getTeamId());
            AlertUtils.info("Succes", "Equipe supprimee.");
            clearFields();
            loadData();
        } catch (Exception e) {
            AlertUtils.error("Erreur", "Suppression echouee: " + e.getMessage());
        }
    }

    @FXML
    private void clearFields() {
        teamsTable.getSelectionModel().clearSelection();
        nameField.clear();
        regionField.clear();
        captainBox.getSelectionModel().clearSelection();
    }
}
