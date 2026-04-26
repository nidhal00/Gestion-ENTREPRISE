package com.gestion.controller;

import com.gestion.entity.Document;
import com.gestion.entity.Entreprise;
import com.gestion.service.DocumentService;
import com.gestion.service.EntrepriseService;
import com.gestion.util.MailService;
import com.gestion.util.PdfExporter;
import com.gestion.util.QRCodeGenerator;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Pane;
import javafx.scene.Scene;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.File;
import java.sql.SQLException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

public class EntrepriseController {

    @FXML private TextField  txtNom, txtMatricule, txtPays,
                             txtEmail, txtTelephone, txtAdresse, txtSearch;
    @FXML private ComboBox<String> cbTaille, cbSecteur;
    @FXML private ComboBox<String> cbFilterStatut, cbFilterSecteur, cbFilterTaille;
    @FXML private DatePicker       dpDateCreation;
    @FXML private Label  errNom, errMatricule, errEmail, errTelephone;
    @FXML private TableView<Entreprise>               tableEntreprises;
    @FXML private TableColumn<Entreprise, String>     colNom, colMatricule,
                                                      colSecteur, colStatut, colCompliance;
    @FXML private HBox adminControls;
    @FXML private Button btnPrev, btnNext;
    @FXML private Label  lblPagination;

    private static final int PAGE_SIZE = 5;
    private int currentPage = 0;

    private final EntrepriseService          service    = new EntrepriseService();
    private final DocumentService            docService = new DocumentService();
    
    private final ObservableList<Entreprise> backupList = FXCollections.observableArrayList();
    private final ObservableList<Entreprise> masterList = FXCollections.observableArrayList();
    private       FilteredList<Entreprise>   filteredList;
    private       SortedList<Entreprise>     sortedList;
    private       Entreprise                 selectedEntreprise = null;
    
    private boolean isUpdating = false; // Prevent recursion in listeners if needed

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^[24579]\\d{7}$");
    private static final Pattern MATRICULE_PATTERN =
            Pattern.compile("^\\d{7}[A-Za-z](/[A-Za-z]){0,2}(/\\d{3})?$");

    private static final List<String> SECTEURS = List.of(
        "Finance & Banque", "Technologie & IT", "Santé & Médical",
        "Agro-alimentaire & Food", "Commerce & Distribution",
        "Industrie & Manufacturing", "Consulting & Services",
        "Immobilier", "Éducation & Formation", "Transport & Logistique",
        "Énergie & Environnement", "Tourisme & Hôtellerie",
        "Médias & Communication", "Juridique & Audit", "Autre"
    );

    public void initialize() {
        SessionManager session = SessionManager.getInstance();

        if (adminControls != null) {
            adminControls.setVisible(session.isAdmin());
            adminControls.setManaged(session.isAdmin());
        }

        cbTaille.setItems(FXCollections.observableArrayList("small", "medium", "large"));
        cbSecteur.setItems(FXCollections.observableArrayList(SECTEURS));

        setupFilterCombos();
        setupTable();
        setupSearchFilter();
        refreshTable();

        tableEntreprises.getSelectionModel().selectedItemProperty()
            .addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    selectedEntreprise = newVal;
                    fillFields(newVal);
                }
            });
    }

    private void setupFilterCombos() {
        List<String> statuts = new ArrayList<>();
        statuts.add("Tous"); statuts.add("en_attente"); statuts.add("validé"); statuts.add("rejeté");
        if (cbFilterStatut != null) cbFilterStatut.setItems(FXCollections.observableArrayList(statuts));

        List<String> secteursAvec = new ArrayList<>();
        secteursAvec.add("Tous"); secteursAvec.addAll(SECTEURS);
        if (cbFilterSecteur != null) cbFilterSecteur.setItems(FXCollections.observableArrayList(secteursAvec));

        List<String> tailles = new ArrayList<>();
        tailles.add("Tous"); tailles.add("small"); tailles.add("medium"); tailles.add("large");
        if (cbFilterTaille != null) cbFilterTaille.setItems(FXCollections.observableArrayList(tailles));
    }

    private void setupTable() {
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colMatricule.setCellValueFactory(new PropertyValueFactory<>("matriculeFiscale"));
        colSecteur.setCellValueFactory(new PropertyValueFactory<>("secteur"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        colCompliance.setCellValueFactory(cell -> {
            Integer score = cell.getValue().getComplianceScore();
            return new SimpleStringProperty(score != null ? score + " %" : "0 %");
        });
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                switch (item) {
                    case "validé"    -> setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
                    case "rejeté"    -> setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                    default          -> setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
                }
            }
        });

        // Numeric comparator for Compliance
        colCompliance.setComparator((s1, s2) -> {
            int v1 = Integer.parseInt(s1.replace(" %", ""));
            int v2 = Integer.parseInt(s2.replace(" %", ""));
            return Integer.compare(v1, v2);
        });
        colCompliance.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                try {
                    int val = Integer.parseInt(item.replace(" %", ""));
                    if      (val >= 75) setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
                    else if (val >= 40) setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
                    else                setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                } catch (NumberFormatException e) {
                    setStyle("-fx-text-fill: #f59e0b;");
                }
            }
        });
    }

    private void setupSearchFilter() {
        filteredList = new FilteredList<>(backupList, p -> true);
        sortedList   = new SortedList<>(filteredList);
        
        // Bind table sorting to global sorted list
        sortedList.comparatorProperty().bind(tableEntreprises.comparatorProperty());
        
        // When filter or sort changes, go back to page 0 and refresh
        sortedList.addListener((javafx.collections.ListChangeListener<Entreprise>) c -> {
            currentPage = 0;
            refreshPage();
        });

        Runnable updatePredicate = () -> {
            String q = txtSearch.getText() == null ? "" : txtSearch.getText().toLowerCase();
            String statut  = (cbFilterStatut  != null && cbFilterStatut.getValue()  != null && !cbFilterStatut.getValue().equals("Tous"))  ? cbFilterStatut.getValue()  : null;
            String secteur = (cbFilterSecteur != null && cbFilterSecteur.getValue() != null && !cbFilterSecteur.getValue().equals("Tous")) ? cbFilterSecteur.getValue() : null;
            String taille  = (cbFilterTaille  != null && cbFilterTaille.getValue()  != null && !cbFilterTaille.getValue().equals("Tous"))  ? cbFilterTaille.getValue()  : null;

            filteredList.setPredicate(ent -> {
                boolean textMatch = q.isEmpty()
                    || ent.getNom().toLowerCase().contains(q)
                    || ent.getMatriculeFiscale().toLowerCase().contains(q)
                    || (ent.getSecteur() != null && ent.getSecteur().toLowerCase().contains(q));
                boolean statutMatch  = statut  == null || statut.equals(ent.getStatut());
                boolean secteurMatch = secteur == null || secteur.equals(ent.getSecteur());
                boolean tailleMatch  = taille  == null || taille.equals(ent.getTaille());
                return textMatch && statutMatch && secteurMatch && tailleMatch;
            });
        };

        txtSearch.textProperty().addListener((obs, o, n) -> updatePredicate.run());
        if (cbFilterStatut  != null) cbFilterStatut.valueProperty().addListener((obs, o, n)  -> updatePredicate.run());
        if (cbFilterSecteur != null) cbFilterSecteur.valueProperty().addListener((obs, o, n) -> updatePredicate.run());
        if (cbFilterTaille  != null) cbFilterTaille.valueProperty().addListener((obs, o, n)  -> updatePredicate.run());

        tableEntreprises.setItems(masterList);
    }

    private void refreshPage() {
        int total = sortedList.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
        currentPage = Math.max(0, Math.min(currentPage, totalPages - 1));

        int start = currentPage * PAGE_SIZE;
        int end   = Math.min(start + PAGE_SIZE, total);

        List<Entreprise> pageData = (start < total) ? sortedList.subList(start, end) : new ArrayList<>();
        masterList.setAll(pageData);

        if (lblPagination != null)
            lblPagination.setText("Page " + (currentPage + 1) + " / " + totalPages);
        if (btnPrev != null) btnPrev.setDisable(currentPage == 0);
        if (btnNext != null) btnNext.setDisable(currentPage >= totalPages - 1);
    }

    @FXML void handlePrevPage() { currentPage--; refreshPage(); }
    @FXML void handleNextPage() { currentPage++; refreshPage(); }

    @FXML void handleResetFilters() {
        txtSearch.clear();
        if (cbFilterStatut  != null) cbFilterStatut.setValue("Tous");
        if (cbFilterSecteur != null) cbFilterSecteur.setValue("Tous");
        if (cbFilterTaille  != null) cbFilterTaille.setValue("Tous");
    }

    @FXML void handleSave() {
        if (!validateInput()) return;
        try {
            SessionManager session = SessionManager.getInstance();
            boolean isNew = (selectedEntreprise == null);
            Entreprise e = isNew ? new Entreprise() : selectedEntreprise;

            updateEntityFromFields(e);

            int autoScore = calculateAutoValidationScore(e);
            boolean alreadyValid = "validé".equals(e.getStatut());

            if (autoScore >= 90) {
                if (!alreadyValid) {
                    e.setStatut("validé");
                    UiHelper.showAlert("Validation automatique",
                        "Félicitations ! Votre entreprise a été validée automatiquement (score " + autoScore + ").",
                        Alert.AlertType.INFORMATION);
                }
            } else if (!alreadyValid) {
                e.setStatut("en_attente");
            }

            if (isNew) {
                e.setOwnerId(session.getUserId());
                service.add(e);
                if (autoScore < 90)
                    UiHelper.showAlert("Entreprise créée !", "Soumise pour validation. Score : " + autoScore + "/90.", Alert.AlertType.INFORMATION);
            } else {
                service.update(e);
                if (alreadyValid || autoScore < 90)
                    UiHelper.showAlert("Succès", "Entreprise mise à jour avec succès.", Alert.AlertType.INFORMATION);
            }

            clearFields();
            refreshTable();
        } catch (SQLException e) {
            UiHelper.showAlert("Erreur BD", "Erreur : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML void handleValidate()   { updateStatus("validé"); }
    @FXML void handleInvalidate() { updateStatus("rejeté"); }

    @FXML void handleDelete() {
        Entreprise sel = tableEntreprises.getSelectionModel().getSelectedItem();
        if (sel == null) {
            UiHelper.showAlert("Sélection requise", "Sélectionnez une entreprise à supprimer.", Alert.AlertType.WARNING);
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Supprimer « " + sel.getNom() + " » ?\nCette action est irréversible.",
            ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmation de suppression");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                try {
                    String name = sel.getNom();
                    int id = sel.getId();
                    service.delete(id);
                    clearFields();
                    refreshTable();
                    UiHelper.showAlert("Supprimé", "Entreprise supprimée avec succès.", Alert.AlertType.INFORMATION);
                } catch (SQLException e) {
                    UiHelper.showAlert("Erreur", "Impossible de supprimer : " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    @FXML void handleExportPdf() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer le PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        fc.setInitialFileName("entreprises.pdf");
        File file = fc.showSaveDialog(tableEntreprises.getScene().getWindow());
        if (file == null) return;
        try {
            Entreprise sel = tableEntreprises.getSelectionModel().getSelectedItem();
            if (sel != null) {
                List<Document> docs = docService.findByEntrepriseId(sel.getId());
                PdfExporter.exportEntreprise(sel, docs, file.getAbsolutePath());
            } else {
                PdfExporter.exportListeEntreprises(sortedList, file.getAbsolutePath());
            }
            UiHelper.showAlert("Export réussi", "PDF généré : " + file.getAbsolutePath(), Alert.AlertType.INFORMATION);
        } catch (Exception ex) {
            UiHelper.showAlert("Erreur PDF", "Impossible de générer le PDF : " + ex.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML void clearFields() {
        txtNom.clear(); txtMatricule.clear();
        txtPays.clear(); txtEmail.clear(); txtTelephone.clear(); txtAdresse.clear();
        cbTaille.getSelectionModel().clearSelection();
        cbSecteur.getSelectionModel().clearSelection();
        dpDateCreation.setValue(null);
        selectedEntreprise = null;
        hideErrors();
    }

    private void updateStatus(String status) {
        if (selectedEntreprise == null) {
            UiHelper.showAlert("Sélection requise", "Sélectionnez d'abord une entreprise.", Alert.AlertType.WARNING);
            return;
        }
        try {
            selectedEntreprise.setStatut(status);
            service.update(selectedEntreprise);

            // Envoi email de notification
            try {
                String email = selectedEntreprise.getEmail();
                if (email != null && !email.isEmpty()) {
                    String nom = selectedEntreprise.getNom();
                    String matricule = selectedEntreprise.getMatriculeFiscale();
                    String secteur = selectedEntreprise.getSecteur() != null ? selectedEntreprise.getSecteur() : "";
                    if ("validé".equals(status)) {
                        MailService.sendEntrepriseValidee(email, nom, matricule, secteur);
                    } else if ("rejeté".equals(status)) {
                        MailService.sendEntrepriseRejetee(email, nom, matricule, secteur);
                    }
                }
            } catch (Exception mailEx) {
                System.err.println("Email non envoyé : " + mailEx.getMessage());
            }

            refreshTable();
            UiHelper.showAlert("Statut mis à jour",
                "L'entreprise \"" + selectedEntreprise.getNom() + "\" est maintenant : " + status
                + ".\nUn email de notification a été envoyé.",
                Alert.AlertType.INFORMATION);
        } catch (SQLException e) {
            UiHelper.showAlert("Erreur", "Impossible de mettre à jour le statut.", Alert.AlertType.ERROR);
        }
    }

    private void refreshTable() {
        try {
            SessionManager session = SessionManager.getInstance();
            List<Entreprise> list = session.isAdmin()
                ? service.findAll()
                : service.findByOwnerId(session.getUserId());

            for (Entreprise ent : list) {
                ent.setComplianceScore(docService.getComplianceScore(ent.getId()));
            }
            backupList.setAll(list);
            refreshPage();
        } catch (SQLException e) {
            UiHelper.showAlert("Erreur", "Impossible de charger les entreprises.", Alert.AlertType.ERROR);
        }
    }

    private void updateEntityFromFields(Entreprise e) {
        e.setNom(txtNom.getText().trim());
        e.setMatriculeFiscale(txtMatricule.getText().trim());
        e.setSecteur(cbSecteur.getValue());
        e.setTaille(cbTaille.getValue());
        e.setPays(txtPays.getText().trim());
        e.setEmail(txtEmail.getText().trim());
        e.setTelephone(txtTelephone.getText().trim());
        e.setAdresse(txtAdresse.getText().trim());
        if (dpDateCreation.getValue() != null) {
            e.setDateCreation(Date.from(
                dpDateCreation.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant()));
        }
    }

    private void fillFields(Entreprise e) {
        UiHelper.resetFieldStyles(txtNom, txtMatricule, txtEmail, txtTelephone);
        txtNom.setText(e.getNom() != null ? e.getNom() : "");
        txtMatricule.setText(e.getMatriculeFiscale() != null ? e.getMatriculeFiscale() : "");
        cbSecteur.setValue(e.getSecteur());
        cbTaille.setValue(e.getTaille());
        txtPays.setText(e.getPays() != null ? e.getPays() : "");
        txtEmail.setText(e.getEmail() != null ? e.getEmail() : "");
        txtTelephone.setText(e.getTelephone() != null ? e.getTelephone() : "");
        txtAdresse.setText(e.getAdresse() != null ? e.getAdresse() : "");
        if (e.getDateCreation() != null) {
            dpDateCreation.setValue(e.getDateCreation().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate());
        } else {
            dpDateCreation.setValue(null);
        }
    }

    private boolean validateInput() {
        hideErrors();
        boolean ok = true;
        if (txtNom.getText().trim().isEmpty()) {
            UiHelper.showError(errNom, "Champ obligatoire", txtNom); ok = false;
        }
        String mat = txtMatricule.getText().trim();
        if (mat.isEmpty()) {
            UiHelper.showError(errMatricule, "Champ obligatoire", txtMatricule); ok = false;
        } else if (!MATRICULE_PATTERN.matcher(mat).matches()) {
            UiHelper.showError(errMatricule, "Format invalide (ex: 1234567A)", txtMatricule); ok = false;
        }
        String email = txtEmail.getText().trim();
        if (email.isEmpty() || !EMAIL_PATTERN.matcher(email).matches()) {
            UiHelper.showError(errEmail, "Email invalide", txtEmail); ok = false;
        }
        String tel = txtTelephone.getText().trim();
        if (tel.isEmpty()) {
            UiHelper.showError(errTelephone, "Champ obligatoire", txtTelephone); ok = false;
        } else if (!PHONE_PATTERN.matcher(tel).matches()) {
            UiHelper.showError(errTelephone, "Exactement 8 chiffres (ex: 71234567)", txtTelephone); ok = false;
        }
        return ok;
    }

    private void hideErrors() {
        UiHelper.hideError(errNom,       txtNom);
        UiHelper.hideError(errMatricule, txtMatricule);
        UiHelper.hideError(errEmail,     txtEmail);
        UiHelper.hideError(errTelephone, txtTelephone);
    }

    private int calculateAutoValidationScore(Entreprise e) {
        int score = 0;
        String sect = e.getSecteur();
        if (sect != null && (sect.equals("Technologie & IT") ||
                             sect.equals("Santé & Médical") ||
                             sect.equals("Agro-alimentaire & Food"))) score += 30;
        if ("large".equals(e.getTaille())) score += 30;
        if (e.getDateCreation() != null) {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            int currentYear = cal.get(java.util.Calendar.YEAR);
            cal.setTime(e.getDateCreation());
            if (cal.get(java.util.Calendar.YEAR) <= (currentYear - 3)) score += 30;
        }
        return score;
    }

    @FXML void handleGenerateQR() {
        Entreprise sel = tableEntreprises.getSelectionModel().getSelectedItem();
        if (sel == null) {
            UiHelper.showAlert("Sélection requise",
                "Sélectionnez une entreprise pour générer son QR Code.", Alert.AlertType.WARNING);
            return;
        }
        try {
            java.awt.image.BufferedImage bi = QRCodeGenerator.generateImage(sel);
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(bi, "PNG", baos);
            Image fxImage = new Image(new java.io.ByteArrayInputStream(baos.toByteArray()));
            showQRDialog(sel, fxImage, bi);
        } catch (Exception ex) {
            UiHelper.showAlert("Erreur QR",
                "Impossible de générer le QR Code : " + ex.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void showQRDialog(Entreprise e, Image fxImage, java.awt.image.BufferedImage bi) {
        Stage dialog = new Stage();
        dialog.setTitle("QR Code — " + e.getNom());
        dialog.initModality(Modality.APPLICATION_MODAL);

        // ── Header ──
        String dotColor = "validé".equals(e.getStatut()) ? "#10b981"
                        : "rejeté".equals(e.getStatut()) ? "#ef4444" : "#f59e0b";

        Label lTitle = new Label("\uD83D\uDD32   Audit Card MindAudit");
        lTitle.setStyle("-fx-text-fill: #e2e8f0; -fx-font-weight: bold; -fx-font-size: 18px;");

        Label lNom = new Label(e.getNom());
        lNom.setStyle("-fx-text-fill: #818cf8; -fx-font-size: 14px; -fx-font-weight: bold;");

        String statutTxt = (e.getStatut() != null ? e.getStatut() : "en_attente")
                         + "  •  " + (e.getComplianceScore() != null ? e.getComplianceScore() : 0) + "% conformité";
        Label lStatut = new Label(statutTxt);
        lStatut.setStyle("-fx-text-fill: " + dotColor + "; -fx-font-size: 11px; -fx-font-weight: bold;");

        VBox header = new VBox(6, lTitle, lNom, lStatut);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(24, 24, 16, 24));

        // ── QR Image (scannable) ──
        ImageView iv = new ImageView(fxImage);
        iv.setFitWidth(280);
        iv.setFitHeight(280);
        iv.setPreserveRatio(true);
        iv.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(99,102,241,0.5), 24, 0, 0, 0);");

        Label hint = new Label("\uD83D\uDCF1  Scannez avec votre smartphone pour accéder à la fiche complète");
        hint.setStyle("-fx-text-fill: #475569; -fx-font-size: 11px;");

        // ── Info badges ──
        HBox badges = new HBox(16);
        badges.setAlignment(Pos.CENTER);
        if (e.getSecteur() != null) {
            Label b1 = new Label("\uD83C\uDFED  " + e.getSecteur());
            b1.setStyle("-fx-background-color: rgba(99,102,241,0.12); -fx-text-fill: #818cf8;"
                + " -fx-padding: 4 10; -fx-background-radius: 20; -fx-font-size: 11px;");
            badges.getChildren().add(b1);
        }
        if (e.getTaille() != null) {
            Label b2 = new Label("\uD83D\uDCCA  " + e.getTaille());
            b2.setStyle("-fx-background-color: rgba(16,185,129,0.1); -fx-text-fill: #34d399;"
                + " -fx-padding: 4 10; -fx-background-radius: 20; -fx-font-size: 11px;");
            badges.getChildren().add(b2);
        }

        // ── Buttons ──
        Button btnSave = new Button("\uD83D\uDCBE  Sauvegarder PNG");
        btnSave.setStyle("-fx-background-color: linear-gradient(to right, #4f46e5, #7c3aed);"
            + " -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8;"
            + " -fx-cursor: hand; -fx-padding: 9 22;");
        btnSave.setOnAction(ev -> {
            try {
                String path = System.getProperty("user.home") + "/Downloads/mindaudit_qr_"
                    + e.getId() + "_" + e.getNom().replaceAll("[^a-zA-Z0-9]", "_") + ".png";
                QRCodeGenerator.saveToFile(bi, path);
                UiHelper.showAlert("Sauvegardé", "QR Code sauvegardé :\n" + path, Alert.AlertType.INFORMATION);
            } catch (Exception ex2) {
                UiHelper.showAlert("Erreur", ex2.getMessage(), Alert.AlertType.ERROR);
            }
        });

        Button btnClose = new Button("Fermer");
        btnClose.setStyle("-fx-background-color: rgba(255,255,255,0.07); -fx-text-fill: #94a3b8;"
            + " -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 9 22;"
            + " -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 8; -fx-border-width: 1;");
        btnClose.setOnAction(ev -> dialog.close());

        HBox buttons = new HBox(12, btnSave, btnClose);
        buttons.setAlignment(Pos.CENTER);
        buttons.setPadding(new Insets(16, 0, 24, 0));

        // ── Separator ──
        Pane sep = new Pane();
        sep.setMaxHeight(1);
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color: rgba(255,255,255,0.06);");

        VBox content = new VBox(10, header, sep, iv, hint, badges, buttons);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(0, 32, 0, 32));
        content.setStyle("-fx-background-color: #0f172a;");

        Scene scene = new Scene(content, 380, 560);
        dialog.setScene(scene);
        dialog.setResizable(false);
        dialog.show();
    }
}
