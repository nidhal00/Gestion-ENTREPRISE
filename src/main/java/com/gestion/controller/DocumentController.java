package com.gestion.controller;

import com.gestion.entity.Document;
import com.gestion.entity.Entreprise;
import com.gestion.service.DocumentService;
import com.gestion.service.EntrepriseService;
import com.gestion.util.SessionManager;
import com.gestion.util.UiHelper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import java.io.File;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

public class DocumentController {

    @FXML private HBox adminControlsDoc, filterBar;
    @FXML private ComboBox<Entreprise> cbEntreprise, cbFilter;
    @FXML private TextField txtNom, txtUrl;
    @FXML private ComboBox<String> cbType;
    @FXML private Label errEntreprise, errNom, errUrl;
    @FXML private TableView<Document> tableDocuments;
    @FXML private TableColumn<Document, String> colNom, colType, colUrl, colStatut;
    @FXML private TableColumn<Document, Date> colDate;

    private DocumentService docService = new DocumentService();
    private EntrepriseService entService = new EntrepriseService();
    private ObservableList<Document> docList = FXCollections.observableArrayList();
    private Document selectedDoc = null;

    public void initialize() {
        SessionManager session = SessionManager.getInstance();
        boolean isAdmin = session.isAdmin();

        if (adminControlsDoc != null) { adminControlsDoc.setVisible(isAdmin); adminControlsDoc.setManaged(isAdmin); }
        if (filterBar != null) { filterBar.setVisible(isAdmin); filterBar.setManaged(isAdmin); }

        setupTable();
        loadEntreprises();
        cbType.setItems(FXCollections.observableArrayList("RH", "fiscal", "financier", "ISO", "Autre"));
        
        if (!isAdmin) restrictUserToOwnCompany(session.getUserId());
        refreshTable();

        tableDocuments.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) { selectedDoc = newVal; fillFields(newVal); }
        });
    }

    private void restrictUserToOwnCompany(int ownerId) {
        try {
            List<Entreprise> myEnts = entService.findByOwnerId(ownerId);
            if (!myEnts.isEmpty()) {
                cbEntreprise.setValue(myEnts.get(0));
                cbEntreprise.setDisable(true);
            } else {
                cbEntreprise.setDisable(true);
                UiHelper.showAlert("Attention", "Créez d'abord votre entreprise.", Alert.AlertType.WARNING);
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void setupTable() {
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateUpload"));
        colUrl.setCellValueFactory(new PropertyValueFactory<>("url"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
    }

    private void loadEntreprises() {
        try {
            List<Entreprise> list = entService.findAll();
            cbEntreprise.setItems(FXCollections.observableArrayList(list));
            cbFilter.setItems(FXCollections.observableArrayList(list));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void refreshTable() {
        try {
            SessionManager s = SessionManager.getInstance();
            if (s.isAdmin()) docList.setAll(docService.findAll());
            else {
                List<Entreprise> myEnts = entService.findByOwnerId(s.getUserId());
                if (!myEnts.isEmpty()) docList.setAll(docService.findByEntrepriseId(myEnts.get(0).getId()));
                else docList.clear();
            }
            tableDocuments.setItems(docList);
        } catch (SQLException e) { UiHelper.showAlert("Erreur", "Chargement impossible", Alert.AlertType.ERROR); }
    }

    @FXML void handleFilter() {
        Entreprise sel = cbFilter.getValue();
        if (sel == null) { refreshTable(); return; }
        try { docList.setAll(docService.findByEntrepriseId(sel.getId())); } 
        catch (SQLException e) { UiHelper.showAlert("Erreur", "Filtre impossible", Alert.AlertType.ERROR); }
    }

    @FXML void handleClearFilter() { cbFilter.getSelectionModel().clearSelection(); refreshTable(); }

    @FXML void handleSave() {
        if (!validateInput()) return;
        try {
            if (selectedDoc == null) {
                Document d = new Document();
                updateEntityFromFields(d);
                d.setDateUpload(new Date());
                d.setStatut("en_attente");
                docService.add(d);
            } else {
                updateEntityFromFields(selectedDoc);
                docService.update(selectedDoc);
            }
            clearFields();
            refreshTable();
        } catch (SQLException e) { UiHelper.showAlert("Erreur", "Echec enregistrement", Alert.AlertType.ERROR); }
    }

    private void updateEntityFromFields(Document d) {
        d.setEntrepriseId(cbEntreprise.getValue().getId());
        d.setNom(txtNom.getText().trim());
        d.setType(cbType.getValue() != null ? cbType.getValue() : "Autre");
        d.setUrl(txtUrl.getText().trim());
    }

    @FXML void handleDelete() {
        Document sel = tableDocuments.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        Alert conf = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer « " + sel.getNom() + " » ?", ButtonType.YES, ButtonType.NO);
        conf.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                try { docService.delete(sel.getId()); clearFields(); refreshTable(); }
                catch (SQLException e) { UiHelper.showAlert("Erreur", "Echec suppression", Alert.AlertType.ERROR); }
            }
        });
    }

    @FXML void handleValidateDoc() { updateDocStatut("validé"); }
    @FXML void handleRejectDoc() { updateDocStatut("rejeté"); }

    private void updateDocStatut(String s) {
        Document sel = tableDocuments.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        try { docService.updateStatut(sel.getId(), s); refreshTable(); } 
        catch (SQLException e) { UiHelper.showAlert("Erreur", "Echec statut", Alert.AlertType.ERROR); }
    }

    @FXML void handleBrowse() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Docs", "*.pdf", "*.docx", "*.xlsx", "*.txt"));
        File f = fc.showOpenDialog(null);
        if (f != null) txtUrl.setText(f.getAbsolutePath());
    }

    @FXML void clearFields() {
        txtNom.clear(); txtUrl.clear(); cbType.getSelectionModel().clearSelection();
        if (SessionManager.getInstance().isAdmin()) cbEntreprise.getSelectionModel().clearSelection();
        selectedDoc = null;
        hideErrors();
    }

    private boolean validateInput() {
        hideErrors();
        boolean v = true;
        if (cbEntreprise.getValue() == null) { UiHelper.showError(errEntreprise, "Requis", cbEntreprise); v = false; }
        if (txtNom.getText().trim().isEmpty()) { UiHelper.showError(errNom, "Requis", txtNom); v = false; }
        if (txtUrl.getText().trim().isEmpty()) { UiHelper.showError(errUrl, "Requis", txtUrl); v = false; }
        return v;
    }

    private void fillFields(Document d) {
        UiHelper.resetFieldStyles(cbEntreprise, txtNom, txtUrl);
        txtNom.setText(d.getNom());
        txtUrl.setText(d.getUrl());
        cbType.setValue(d.getType());
        if (SessionManager.getInstance().isAdmin()) {
            cbEntreprise.getItems().stream().filter(e -> e.getId() == d.getEntrepriseId()).findFirst().ifPresent(cbEntreprise::setValue);
        }
    }

    private void hideErrors() { 
        UiHelper.hideError(errEntreprise, cbEntreprise);
        UiHelper.hideError(errNom, txtNom);
        UiHelper.hideError(errUrl, txtUrl);
    }
}
