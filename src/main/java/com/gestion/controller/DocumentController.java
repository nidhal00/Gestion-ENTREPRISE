package com.gestion.controller;

import com.gestion.entity.Document;
import com.gestion.entity.Entreprise;
import com.gestion.service.DocumentService;
import com.gestion.service.EntrepriseService;
import com.gestion.util.MailService;
import com.gestion.util.SessionManager;
import com.gestion.util.UiHelper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Optional;
import java.util.List;

public class DocumentController {

    @FXML private HBox adminControlsDoc, filterBar;
    @FXML private ComboBox<Entreprise> cbEntreprise, cbFilter;
    @FXML private ComboBox<String>     cbFilterStatutDoc, cbFilterTypeDoc;
    @FXML private TextField            txtNom, txtUrl, txtSearchDoc;
    @FXML private ComboBox<String>     cbType;
    @FXML private Label                errEntreprise, errNom, errType, errUrl;
    @FXML private TableView<Document>           tableDocuments;
    @FXML private TableColumn<Document, String> colNom, colType, colUrl, colStatut;
    @FXML private TableColumn<Document, Date>   colDate;
    @FXML private Button btnPrev, btnNext;
    @FXML private Label  lblPagination;

    private static final int PAGE_SIZE = 5;
    private int currentPage = 0;

    private final DocumentService            docService = new DocumentService();
    private final EntrepriseService          entService = new EntrepriseService();
    
    private final ObservableList<Document>   backupList  = FXCollections.observableArrayList();
    private final ObservableList<Document>   masterList  = FXCollections.observableArrayList();
    private       FilteredList<Document>     filteredList;
    private       SortedList<Document>       sortedList;
    private       Document                   selectedDoc = null;

    public void initialize() {
        SessionManager session = SessionManager.getInstance();
        boolean admin = session.isAdmin();

        if (adminControlsDoc != null) { adminControlsDoc.setVisible(admin); adminControlsDoc.setManaged(admin); }
        if (filterBar != null)        { filterBar.setVisible(admin);        filterBar.setManaged(admin); }

        setupTable();
        loadEntreprises();
        setupFilterCombos();
        
        cbType.setItems(FXCollections.observableArrayList("RH", "fiscal", "financier", "ISO", "Autre"));

        if (!admin) restrictUserToOwnCompany(session.getUserId());
        
        setupSearchFilter();
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

    private void setupFilterCombos() {
        List<String> statuts = List.of("Tous", "en_attente", "validé", "rejeté");
        if (cbFilterStatutDoc != null) cbFilterStatutDoc.setItems(FXCollections.observableArrayList(statuts));

        List<String> types = List.of("Tous", "RH", "fiscal", "financier", "ISO", "Autre");
        if (cbFilterTypeDoc != null) cbFilterTypeDoc.setItems(FXCollections.observableArrayList(types));
    }

    private void setupSearchFilter() {
        filteredList = new FilteredList<>(backupList, p -> true);
        sortedList   = new SortedList<>(filteredList);

        // Bind table sorting to global sorted list
        sortedList.comparatorProperty().bind(tableDocuments.comparatorProperty());

        // Refresh page when filter or sort changes
        sortedList.addListener((javafx.collections.ListChangeListener<Document>) c -> {
            currentPage = 0;
            refreshPage();
        });

        Runnable updatePredicate = () -> {
            String q = (txtSearchDoc != null && txtSearchDoc.getText() != null) ? txtSearchDoc.getText().toLowerCase() : "";
            Entreprise entFilter = cbFilter.getValue();
            String statutFilter = (cbFilterStatutDoc != null && cbFilterStatutDoc.getValue() != null && !cbFilterStatutDoc.getValue().equals("Tous")) ? cbFilterStatutDoc.getValue() : null;
            String typeFilter = (cbFilterTypeDoc != null && cbFilterTypeDoc.getValue() != null && !cbFilterTypeDoc.getValue().equals("Tous")) ? cbFilterTypeDoc.getValue() : null;

            filteredList.setPredicate(doc -> {
                boolean textMatch = q.isEmpty() || doc.getNom().toLowerCase().contains(q);
                boolean entMatch = entFilter == null || doc.getEntrepriseId() == entFilter.getId();
                boolean statutMatch = statutFilter == null || statutFilter.equals(doc.getStatut());
                boolean typeMatch = typeFilter == null || typeFilter.equals(doc.getType());
                return textMatch && entMatch && statutMatch && typeMatch;
            });
        };

        if (txtSearchDoc != null) txtSearchDoc.textProperty().addListener((obs, ov, nv) -> updatePredicate.run());
        cbFilter.valueProperty().addListener((obs, ov, nv) -> updatePredicate.run());
        if (cbFilterStatutDoc != null) cbFilterStatutDoc.valueProperty().addListener((obs, ov, nv) -> updatePredicate.run());
        if (cbFilterTypeDoc != null) cbFilterTypeDoc.valueProperty().addListener((obs, ov, nv) -> updatePredicate.run());

        tableDocuments.setItems(masterList);
    }

    private void refreshPage() {
        int total = sortedList.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
        currentPage = Math.max(0, Math.min(currentPage, totalPages - 1));

        int start = currentPage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, total);

        List<Document> pageData = (start < total) ? sortedList.subList(start, end) : new ArrayList<>();
        masterList.setAll(pageData);

        if (lblPagination != null)
            lblPagination.setText("Page " + (currentPage + 1) + " / " + totalPages);
        if (btnPrev != null) btnPrev.setDisable(currentPage == 0);
        if (btnNext != null) btnNext.setDisable(currentPage >= totalPages - 1);
    }

    @FXML void handlePrevPage() { currentPage--; refreshPage(); }
    @FXML void handleNextPage() { currentPage++; refreshPage(); }

    @FXML void handleResetFilters() {
        if (txtSearchDoc != null) txtSearchDoc.clear();
        cbFilter.setValue(null);
        if (cbFilterStatutDoc != null) cbFilterStatutDoc.setValue("Tous");
        if (cbFilterTypeDoc   != null) cbFilterTypeDoc.setValue("Tous");
        // Predicate listeners will fire automatically; also reload from DB
        refreshTable();
    }

    private void refreshTable() {
        try {
            SessionManager s = SessionManager.getInstance();
            List<Document> list;
            if (s.isAdmin()) {
                list = docService.findAll();
            } else {
                List<Entreprise> myEnts = entService.findByOwnerId(s.getUserId());
                if (!myEnts.isEmpty()) list = docService.findByEntrepriseId(myEnts.get(0).getId());
                else list = new ArrayList<>();
            }
            backupList.setAll(list);
            refreshPage();
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
                    String name = sel.getNom();
                    int id = sel.getId();
                    docService.delete(id);
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

    @FXML void handleRejectDoc() {
        Document sel = tableDocuments.getSelectionModel().getSelectedItem();
        if (sel == null) {
            UiHelper.showAlert("Sélection requise", "Sélectionnez un document.", Alert.AlertType.WARNING);
            return;
        }
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Motif de rejet");
        dialog.setHeaderText("Document : " + sel.getNom());
        dialog.setContentText("Motif du rejet (obligatoire) :");
        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty() || result.get().trim().isEmpty()) {
            UiHelper.showAlert("Motif requis", "Veuillez saisir un motif de rejet.", Alert.AlertType.WARNING);
            return;
        }
        String motif = result.get().trim();
        try {
            docService.updateStatut(sel.getId(), "rejeté");
            sel.setAnalysisReport(motif);
            sel.setStatut("rejeté");
            docService.update(sel);
            try {
                Entreprise e = entService.findById(sel.getEntrepriseId());
                if (e != null && e.getEmail() != null && !e.getEmail().isEmpty()) {
                    String typeDoc = sel.getType() != null ? sel.getType() : "Document";
                    MailService.sendDocumentRejete(
                        e.getEmail(), sel.getNom(), typeDoc, e.getNom(), motif);
                }
            } catch (Exception mailEx) {
                System.err.println("Email non envoyé : " + mailEx.getMessage());
            }
            refreshTable();
            UiHelper.showAlert("Document rejeté",
                "Document rejeté avec succès. Motif enregistré. Email envoyé.",
                Alert.AlertType.INFORMATION);
        } catch (SQLException ex) {
            UiHelper.showAlert("Erreur", ex.getMessage(), Alert.AlertType.ERROR);
        }
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

            // Envoi email HTML branded
            try {
                Entreprise e = entService.findById(sel.getEntrepriseId());
                if (e != null && e.getEmail() != null && !e.getEmail().isEmpty()) {
                    String typeDoc = sel.getType() != null ? sel.getType() : "Document";
                    if ("validé".equals(statut)) {
                        MailService.sendDocumentValide(
                            e.getEmail(), sel.getNom(), typeDoc, e.getNom());
                    }
                }
            } catch (Exception mailEx) {
                System.err.println("Email non envoyé : " + mailEx.getMessage());
            }

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
