package com.gestion.controller;

import com.gestion.entity.Entreprise;
import com.gestion.service.DocumentService;
import com.gestion.service.EntrepriseService;
import com.gestion.util.SessionManager;
import com.gestion.util.UiHelper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class EntrepriseController {

    @FXML private TextField txtNom, txtMatricule, txtSecteur, txtPays, txtEmail, txtTelephone, txtAdresse, txtSearch;
    @FXML private ComboBox<String> cbTaille;
    @FXML private Label errNom, errMatricule, errEmail, errTelephone;
    @FXML private TableView<Entreprise> tableEntreprises;
    @FXML private TableColumn<Entreprise, String> colNom, colMatricule, colSecteur, colStatut, colCompliance;
    @FXML private HBox adminControls;

    private EntrepriseService service = new EntrepriseService();
    private DocumentService docService = new DocumentService();
    private ObservableList<Entreprise> entrepriseList = FXCollections.observableArrayList();
    private Entreprise selectedEntreprise = null;
    private static final List<String> CORE_CATEGORIES = Arrays.asList("RH", "fiscal", "financier", "ISO");

    public void initialize() {
        if (adminControls != null) {
            adminControls.setVisible(SessionManager.getInstance().isAdmin());
            adminControls.setManaged(SessionManager.getInstance().isAdmin());
        }

        cbTaille.setItems(FXCollections.observableArrayList("small", "medium", "large"));
        setupTable();
        refreshTable();
        setupSearchFilter();
        
        tableEntreprises.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedEntreprise = newVal;
                fillFields(newVal);
            }
        });
    }

    private void setupTable() {
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colMatricule.setCellValueFactory(new PropertyValueFactory<>("matriculeFiscale"));
        colSecteur.setCellValueFactory(new PropertyValueFactory<>("secteur"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        
        colCompliance.setCellValueFactory(cellData -> {
            Integer score = cellData.getValue().getComplianceScore();
            return new SimpleStringProperty(score != null ? score + " %" : "0 %");
        });

        colCompliance.setCellFactory(column -> new TableCell<Entreprise, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    int val = Integer.parseInt(item.replace(" %", ""));
                    if (val >= 75) setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
                    else if (val >= 40) setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
                    else setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                }
            }
        });
    }

    private void setupSearchFilter() {
        FilteredList<Entreprise> filteredData = new FilteredList<>(entrepriseList, p -> true);
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(ent -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();
                return ent.getNom().toLowerCase().contains(lowerCaseFilter) ||
                       ent.getMatriculeFiscale().toLowerCase().contains(lowerCaseFilter) ||
                       (ent.getSecteur() != null && ent.getSecteur().toLowerCase().contains(lowerCaseFilter));
            });
        });
        SortedList<Entreprise> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableEntreprises.comparatorProperty());
        tableEntreprises.setItems(sortedData);
    }

    private void refreshTable() {
        try {
            SessionManager session = SessionManager.getInstance();
            List<Entreprise> list = session.isAdmin() ? service.findAll() : service.findByOwnerId(session.getUserId());
            calculateComplianceScores(list);
            entrepriseList.setAll(list);
        } catch (SQLException e) {
            UiHelper.showAlert("Erreur", "Impossible de charger les entreprises", Alert.AlertType.ERROR);
        }
    }

    private void calculateComplianceScores(List<Entreprise> list) throws SQLException {
        Map<Integer, List<String>> categoriesMap = docService.getCategoriesPerEntreprise();
        for (Entreprise ent : list) {
            List<String> types = categoriesMap.get(ent.getId());
            if (types == null || types.isEmpty()) {
                ent.setComplianceScore(0);
                continue;
            }
            long uniqueCoreCount = types.stream().distinct().filter(CORE_CATEGORIES::contains).count();
            ent.setComplianceScore((int) ((uniqueCoreCount / (double) CORE_CATEGORIES.size()) * 100));
        }
    }

    @FXML
    void handleSave() {
        if (!validateInput()) return;
        try {
            if (selectedEntreprise == null) {
                Entreprise e = new Entreprise();
                updateEntityFromFields(e);
                e.setStatut("en_attente");
                e.setOwnerId(SessionManager.getInstance().getUserId());
                service.add(e);
            } else {
                updateEntityFromFields(selectedEntreprise);
                service.update(selectedEntreprise);
            }
            clearFields();
            refreshTable();
        } catch (SQLException e) {
            UiHelper.showAlert("Erreur", "Erreur lors de l'enregistrement", Alert.AlertType.ERROR);
        }
    }

    @FXML void handleValidate() { updateStatus("validé"); }
    @FXML void handleInvalidate() { updateStatus("rejeté"); }

    private void updateStatus(String status) {
        if (selectedEntreprise == null) return;
        try {
            selectedEntreprise.setStatut(status);
            service.update(selectedEntreprise);
            refreshTable();
        } catch (SQLException e) {
            UiHelper.showAlert("Erreur", "Impossible de mettre à jour le statut.", Alert.AlertType.ERROR);
        }
    }

    private void updateEntityFromFields(Entreprise e) {
        e.setNom(txtNom.getText());
        e.setMatriculeFiscale(txtMatricule.getText());
        e.setSecteur(txtSecteur.getText());
        e.setTaille(cbTaille.getValue());
        e.setPays(txtPays.getText());
        e.setEmail(txtEmail.getText());
        e.setTelephone(txtTelephone.getText());
        e.setAdresse(txtAdresse.getText());
    }

    @FXML
    void handleDelete() {
        Entreprise selected = tableEntreprises.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer l'entreprise " + selected.getNom() + " ?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    service.delete(selected.getId());
                    refreshTable();
                    clearFields();
                } catch (SQLException e) {
                    UiHelper.showAlert("Erreur", "Impossible de supprimer.", Alert.AlertType.ERROR);
                }
            }
        });
    }

    @FXML
    void clearFields() {
        txtNom.clear(); txtMatricule.clear(); txtSecteur.clear();
        cbTaille.getSelectionModel().clearSelection();
        txtPays.clear(); txtEmail.clear(); txtTelephone.clear(); txtAdresse.clear();
        selectedEntreprise = null;
        hideErrors();
    }

    private boolean validateInput() {
        hideErrors();
        boolean isValid = true;
        if (txtNom.getText().trim().isEmpty()) { UiHelper.showError(errNom, "Champ obligatoire", txtNom); isValid = false; }
        if (txtMatricule.getText().trim().isEmpty()) { UiHelper.showError(errMatricule, "Champ obligatoire", txtMatricule); isValid = false; }
        if (!Pattern.matches("^[A-Za-z0-9+_.-]+@(.+)$", txtEmail.getText())) { UiHelper.showError(errEmail, "Email invalide", txtEmail); isValid = false; }
        return isValid;
    }

    private void fillFields(Entreprise e) {
        UiHelper.resetFieldStyles(txtNom, txtMatricule, txtEmail);
        txtNom.setText(e.getNom());
        txtMatricule.setText(e.getMatriculeFiscale());
        txtSecteur.setText(e.getSecteur());
        cbTaille.setValue(e.getTaille());
        txtPays.setText(e.getPays());
        txtEmail.setText(e.getEmail());
        txtTelephone.setText(e.getTelephone());
        txtAdresse.setText(e.getAdresse());
    }

    private void hideErrors() {
        UiHelper.hideError(errNom, txtNom);
        UiHelper.hideError(errMatricule, txtMatricule);
        UiHelper.hideError(errEmail, txtEmail);
        UiHelper.hideError(errTelephone, txtTelephone);
    }
}
