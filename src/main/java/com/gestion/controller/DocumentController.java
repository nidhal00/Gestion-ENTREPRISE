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
    @FXML private TextField            txtNom, txtUrl;
    @FXML private ComboBox<String>     cbType;
    @FXML private Label                errEntreprise, errNom, errType, errUrl;
    @FXML private TableView<Document>           tableDocuments;
    @FXML private TableColumn<Document, String> colNom, colType, colUrl, colStatut;
    @FXML private TableColumn<Document, Date>   colDate;

    private final DocumentService            docService = new DocumentService();
    private final EntrepriseService          entService = new EntrepriseService();
    private final ObservableList<Document>   docList    = FXCollections.observableArrayList();
    private       Document                   selectedDoc = null;

    public void initialize() {
        SessionManager session = SessionManager.getInstance();
        boolean admin = session.isAdmin();

        if (adminControlsDoc != null) { adminControlsDoc.setVisible(admin); adminControlsDoc.setManaged(admin); }
        if (filterBar != null)        { filterBar.setVisible(admin);        filterBar.setManaged(admin); }

        setupTable();
        loadEntreprises();
        cbType.setItems(FXCollections.observableArrayList("RH", "fiscal", "financier", "ISO", "Autre"));

        if (!admin) restrictUserToOwnCompany(session.getUserId());
        refreshTable();

        tableDocuments.getSelectionModel().selectedItemProperty()
            .addListener((obs, oldVal, newVal) -> {
                if (newVal != null) { selectedDoc = newVal; fillFields(newVal); }
            });
    }

    private void setupTable() {
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateUpload"));
        colUrl.setCellValueFactory(new PropertyValueFactory<>("url"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                switch (item) {
                    case "validé"  -> setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
                    case "rejeté"  -> setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                    default        -> setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
                }
            }
        });
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
            if (s.isAdmin()) {
                docList.setAll(docService.findAll());
            } else {
                List<Entreprise> myEnts = entService.findByOwnerId(s.getUserId());
                if (!myEnts.isEmpty()) docList.setAll(docService.findByEntrepriseId(myEnts.get(0).getId()));
                else docList.clear();
            }
            tableDocuments.setItems(docList);
        } catch (SQLException e) {
            UiHelper.showAlert("Erreur", "Chargement des documents impossible.", Alert.AlertType.ERROR);
        }
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

    @FXML void handleSave() {
        if (!validateInput()) return;
        try {
            if (selectedDoc == null) {
                Document d = new Document();
                updateEntityFromFields(d);
                d.setDateUpload(new Date());
                d.setStatut("en_attente");
                docService.add(d);
                UiHelper.showAlert("Succès", "Document ajouté avec succès.", Alert.AlertType.INFORMATION);
            } else {
                updateEntityFromFields(selectedDoc);
                docService.update(selectedDoc);
                UiHelper.showAlert("Succès", "Document mis à jour.", Alert.AlertType.INFORMATION);
            }
            clearFields();
            refreshTable();
        } catch (SQLException e) {
            UiHelper.showAlert("Erreur", "Échec de l'enregistrement : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML void handleDelete() {
        Document sel = tableDocuments.getSelectionModel().getSelectedItem();
        if (sel == null) {
            UiHelper.showAlert("Sélection requise", "Sélectionnez un document.", Alert.AlertType.WARNING);
            return;
        }
        Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
            "Supprimer « " + sel.getNom() + " » ?",
            ButtonType.YES, ButtonType.NO);
        conf.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                try {
                    docService.delete(sel.getId());
                    clearFields();
                    refreshTable();
                    UiHelper.showAlert("Supprimé", "Document supprimé.", Alert.AlertType.INFORMATION);
                } catch (SQLException e) {
                    UiHelper.showAlert("Erreur", "Échec suppression : " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    @FXML void handleValidateDoc() { updateDocStatut("validé"); }
    @FXML void handleRejectDoc()   { updateDocStatut("rejeté"); }

    @FXML void handleFilter() {
        Entreprise sel = cbFilter.getValue();
        if (sel == null) { refreshTable(); return; }
        try { docList.setAll(docService.findByEntrepriseId(sel.getId())); }
        catch (SQLException e) { UiHelper.showAlert("Erreur", "Filtre impossible.", Alert.AlertType.ERROR); }
    }

    @FXML void handleClearFilter() {
        cbFilter.getSelectionModel().clearSelection();
        refreshTable();
    }

    @FXML void handleBrowse() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Sélectionner un document");
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Documents", "*.pdf", "*.docx", "*.xlsx", "*.txt"));
        File f = fc.showOpenDialog(null);
        if (f != null) txtUrl.setText(f.getAbsolutePath());
    }

    @FXML void clearFields() {
        txtNom.clear(); txtUrl.clear();
        cbType.getSelectionModel().clearSelection();
        if (SessionManager.getInstance().isAdmin())
            cbEntreprise.getSelectionModel().clearSelection();
        selectedDoc = null;
        hideErrors();
    }

    private void updateDocStatut(String statut) {
        Document sel = tableDocuments.getSelectionModel().getSelectedItem();
        if (sel == null) {
            UiHelper.showAlert("Sélection requise", "Sélectionnez un document.", Alert.AlertType.WARNING);
            return;
        }
        try {
            docService.updateStatut(sel.getId(), statut);
            refreshTable();
        } catch (SQLException e) {
            UiHelper.showAlert("Erreur", "Impossible de mettre à jour le statut.", Alert.AlertType.ERROR);
        }
    }

    private void updateEntityFromFields(Document d) {
        d.setEntrepriseId(cbEntreprise.getValue().getId());
        d.setNom(txtNom.getText().trim());
        d.setType(cbType.getValue() != null ? cbType.getValue() : "Autre");
        d.setUrl(txtUrl.getText().trim());
    }

    private void fillFields(Document d) {
        UiHelper.resetFieldStyles(cbEntreprise, txtNom, txtUrl);
        txtNom.setText(d.getNom());
        txtUrl.setText(d.getUrl());
        cbType.setValue(d.getType());
        if (SessionManager.getInstance().isAdmin()) {
            cbEntreprise.getItems().stream()
                .filter(e -> e.getId() == d.getEntrepriseId())
                .findFirst()
                .ifPresent(cbEntreprise::setValue);
        }
    }

    private boolean validateInput() {
        hideErrors();
        boolean v = true;

        if (cbEntreprise.getValue() == null) {
            UiHelper.showError(errEntreprise, "Veuillez sélectionner une entreprise", cbEntreprise);
            v = false;
        }
        if (txtNom.getText().trim().isEmpty()) {
            UiHelper.showError(errNom, "Veuillez saisir un libellé", txtNom);
            v = false;
        }
        if (cbType.getValue() == null) {
            UiHelper.showError(errType, "Veuillez sélectionner une catégorie", cbType);
            v = false;
        }
        if (txtUrl.getText().trim().isEmpty()) {
            UiHelper.showError(errUrl, "Veuillez sélectionner un fichier", txtUrl);
            v = false;
        }

        return v;
    }

    private void hideErrors() {
        UiHelper.hideError(errEntreprise, cbEntreprise);
        UiHelper.hideError(errNom,        txtNom);
        UiHelper.hideError(errType,       cbType);
        UiHelper.hideError(errUrl,        txtUrl);
    }
}
