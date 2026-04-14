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
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

public class EntrepriseController {

    @FXML private TextField  txtNom, txtMatricule, txtPays,
                             txtEmail, txtTelephone, txtAdresse, txtSearch;
    @FXML private ComboBox<String> cbTaille;
    @FXML private ComboBox<String> cbSecteur;
    @FXML private DatePicker       dpDateCreation;
    @FXML private Label  errNom, errMatricule, errEmail, errTelephone;
    @FXML private TableView<Entreprise>               tableEntreprises;
    @FXML private TableColumn<Entreprise, String>     colNom, colMatricule,
                                                      colSecteur, colStatut, colCompliance;
    @FXML private HBox adminControls;

    private final EntrepriseService          service    = new EntrepriseService();
    private final DocumentService            docService = new DocumentService();
    private final ObservableList<Entreprise> entrepriseList = FXCollections.observableArrayList();
    private       Entreprise                 selectedEntreprise = null;

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^[24579]\\d{7}$");

    /**
     * Matricule fiscal tunisien.
     * Obligatoire : 7 chiffres + 1 lettre  (ex: 1234567A)
     * Optionnel   : 0 à 2 fois /lettre     (ex: /P  ou  /P/M)
     * Optionnel   : /3chiffres             (ex: /000)
     * Exemples valides : 1234567A | 1234567A/P | 1234567A/P/M/000 | 1234567A/000
     */
    private static final Pattern MATRICULE_PATTERN =
            Pattern.compile("^\\d{7}[A-Za-z](/[A-Za-z]){0,2}(/\\d{3})?$");

    /** Secteurs d'activité professionnels */
    private static final List<String> SECTEURS = List.of(
        "Finance & Banque",
        "Technologie & IT",
        "Santé & Médical",
        "Agro-alimentaire & Food",
        "Commerce & Distribution",
        "Industrie & Manufacturing",
        "Consulting & Services",
        "Immobilier",
        "Éducation & Formation",
        "Transport & Logistique",
        "Énergie & Environnement",
        "Tourisme & Hôtellerie",
        "Médias & Communication",
        "Juridique & Audit",
        "Autre"
    );

    public void initialize() {
        SessionManager session = SessionManager.getInstance();

        if (adminControls != null) {
            adminControls.setVisible(session.isAdmin());
            adminControls.setManaged(session.isAdmin());
        }

        cbTaille.setItems(FXCollections.observableArrayList("small", "medium", "large"));
        cbSecteur.setItems(FXCollections.observableArrayList(SECTEURS));

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

    private void setupTable() {
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colMatricule.setCellValueFactory(new PropertyValueFactory<>("matriculeFiscale"));
        colSecteur.setCellValueFactory(new PropertyValueFactory<>("secteur"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        // Colonne conformité colorée
        colCompliance.setCellValueFactory(cell -> {
            Integer score = cell.getValue().getComplianceScore();
            return new SimpleStringProperty(score != null ? score + " %" : "0 %");
        });
        colCompliance.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                int val = Integer.parseInt(item.replace(" %", ""));
                if      (val >= 75) setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
                else if (val >= 40) setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
                else                setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
            }
        });
    }

    private void setupSearchFilter() {
        FilteredList<Entreprise> filtered = new FilteredList<>(entrepriseList, p -> true);
        txtSearch.textProperty().addListener((obs, oldVal, newVal) ->
            filtered.setPredicate(ent -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String q = newVal.toLowerCase();
                return ent.getNom().toLowerCase().contains(q)
                    || ent.getMatriculeFiscale().toLowerCase().contains(q)
                    || (ent.getSecteur() != null && ent.getSecteur().toLowerCase().contains(q));
            }));
        SortedList<Entreprise> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(tableEntreprises.comparatorProperty());
        tableEntreprises.setItems(sorted);
    }


    @FXML void handleSave() {
        if (!validateInput()) return;
        try {
            SessionManager session = SessionManager.getInstance();
            boolean isNew = (selectedEntreprise == null);
            Entreprise e = isNew ? new Entreprise() : selectedEntreprise;
            
            updateEntityFromFields(e);

            // Calcul du score d'éligibilité pour validation directe
            int autoScore = calculateAutoValidationScore(e);
            boolean alreadyValid = "validé".equals(e.getStatut());

            if (autoScore >= 90) {
                if (!alreadyValid) {
                    e.setStatut("validé");
                    UiHelper.showAlert("Validation automatique",
                        "Félicitations ! Votre entreprise a été validée automatiquement grâce à son score (90+ pts).",
                        Alert.AlertType.INFORMATION);
                }
            } else if (!alreadyValid) {
                // Reste en attente s'il n'était pas déjà validé
                e.setStatut("en_attente");
            }

            if (isNew) {
                e.setOwnerId(session.getUserId());
                service.add(e);
                if (autoScore < 90) {
                    UiHelper.showAlert("Entreprise créée !",
                        "Soumise pour validation. Score : " + autoScore + "/90 pour validation directe.",
                        Alert.AlertType.INFORMATION);
                }
            } else {
                service.update(e);
                if (!alreadyValid && autoScore >= 90) {
                    // Alert already shown above
                } else {
                    UiHelper.showAlert("Succès", "Entreprise mise à jour avec succès.", Alert.AlertType.INFORMATION);
                }
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
                    service.delete(sel.getId());
                    clearFields();
                    refreshTable();
                    UiHelper.showAlert("Supprimé", "Entreprise supprimée avec succès.", Alert.AlertType.INFORMATION);
                } catch (SQLException e) {
                    UiHelper.showAlert("Erreur", "Impossible de supprimer : " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
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

    // ── Status update ──────────────────────────────────────────
    private void updateStatus(String status) {
        if (selectedEntreprise == null) {
            UiHelper.showAlert("Sélection requise", "Sélectionnez d'abord une entreprise.", Alert.AlertType.WARNING);
            return;
        }
        try {
            selectedEntreprise.setStatut(status);
            service.update(selectedEntreprise);
            refreshTable();
        } catch (SQLException e) {
            UiHelper.showAlert("Erreur", "Impossible de mettre à jour le statut.", Alert.AlertType.ERROR);
        }
    }

    private void refreshTable() {
        try {
            SessionManager session = SessionManager.getInstance();
            List<Entreprise> list  = session.isAdmin()
                ? service.findAll()
                : service.findByOwnerId(session.getUserId());

            for (Entreprise ent : list) {
                ent.setComplianceScore(docService.getComplianceScore(ent.getId()));
            }
            entrepriseList.setAll(list);
        } catch (SQLException e) {
            UiHelper.showAlert("Erreur", "Impossible de charger les entreprises.", Alert.AlertType.ERROR);
        }
    }

    // ── Field mapping ──────────────────────────────────────────
    private void updateEntityFromFields(Entreprise e) {
        e.setNom(txtNom.getText().trim());
        e.setMatriculeFiscale(txtMatricule.getText().trim());
        e.setSecteur(cbSecteur.getValue());
        e.setTaille(cbTaille.getValue());
        e.setPays(txtPays.getText().trim());
        e.setEmail(txtEmail.getText().trim());
        e.setTelephone(txtTelephone.getText().trim());
        e.setAdresse(txtAdresse.getText().trim());
        // Date de création depuis le DatePicker
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
        // Remplir le DatePicker si une date existe
        if (e.getDateCreation() != null) {
            dpDateCreation.setValue(e.getDateCreation().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate());
        } else {
            dpDateCreation.setValue(null);
        }
    }

    // ── Validation ─────────────────────────────────────────────
    private boolean validateInput() {
        hideErrors();
        boolean ok = true;

        // Nom
        if (txtNom.getText().trim().isEmpty()) {
            UiHelper.showError(errNom, "Champ obligatoire", txtNom);
            ok = false;
        }

        // Matricule fiscal tunisien : 7 chiffres + 1 lettre [+ /X/X/XXX]
        String mat = txtMatricule.getText().trim();
        if (mat.isEmpty()) {
            UiHelper.showError(errMatricule, "Champ obligatoire", txtMatricule);
            ok = false;
        } else if (!MATRICULE_PATTERN.matcher(mat).matches()) {
            UiHelper.showError(errMatricule, "Format invalide (ex: 1234567A ou 1234567A/P/M/000)", txtMatricule);
            ok = false;
        }

        // Email
        String email = txtEmail.getText().trim();
        if (email.isEmpty() || !EMAIL_PATTERN.matcher(email).matches()) {
            UiHelper.showError(errEmail, "Email invalide (ex: nom@domaine.tn)", txtEmail);
            ok = false;
        }

        // Téléphone : obligatoire, exactement 8 chiffres
        String tel = txtTelephone.getText().trim();
        if (tel.isEmpty()) {
            UiHelper.showError(errTelephone, "Champ obligatoire", txtTelephone);
            ok = false;
        } else if (!PHONE_PATTERN.matcher(tel).matches()) {
            UiHelper.showError(errTelephone, "Exactement 8 chiffres requis (ex: 71234567)", txtTelephone);
            ok = false;
        }

        return ok;
    }

    private void hideErrors() {
        UiHelper.hideError(errNom,       txtNom);
        UiHelper.hideError(errMatricule, txtMatricule);
        UiHelper.hideError(errEmail,     txtEmail);
        UiHelper.hideError(errTelephone, txtTelephone);
    }

    /**
     * Calcule un score automatique pour la validation directe.
     * +30 pts if IT/Medical/Food
     * +30 pts if Large
     * +30 pts if Creation Date > 3 years (<= 2023)
     */
    private int calculateAutoValidationScore(Entreprise e) {
        int score = 0;

        // Condition 1: Secteur (IT, Medicin, Food)
        String sect = e.getSecteur();
        if (sect != null && (sect.equals("Technologie & IT") ||
                             sect.equals("Santé & Médical") ||
                             sect.equals("Agro-alimentaire & Food"))) {
            score += 30;
        }

        // Condition 2: Taille (Large)
        if ("large".equals(e.getTaille())) {
            score += 30;
        }

        // Condition 3: Ancienneté (> 3 ans)
        if (e.getDateCreation() != null) {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            int currentYear = cal.get(java.util.Calendar.YEAR);
            cal.setTime(e.getDateCreation());
            int creationYear = cal.get(java.util.Calendar.YEAR);

            if (creationYear <= (currentYear - 3)) {
                score += 30;
            }
        }

        return score;
    }
}
