package com.gestion.controller;

import com.gestion.entity.Document;
import com.gestion.entity.Entreprise;
import com.gestion.service.DocumentService;
import com.gestion.service.EntrepriseService;
import com.gestion.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class HistoriqueController {

    @FXML private ScrollPane scrollPane;
    @FXML private VBox vboxTimeline;
    @FXML private ComboBox<String> cbFilterType;
    @FXML private ComboBox<String> cbFilterStatut;
    @FXML private DatePicker dpFrom;
    @FXML private DatePicker dpTo;
    @FXML private Button btnReset;
    @FXML private Label lblCount;

    private final EntrepriseService entService = new EntrepriseService();
    private final DocumentService docService = new DocumentService();
    private final List<HistoriqueEntry> allEntries = new ArrayList<>();

    private static class HistoriqueEntry {
        Date date;
        String type;
        String nom;
        String statut;
        String detail;
        int entityId;
    }

    public void initialize() {
        cbFilterType.setItems(FXCollections.observableArrayList("Tous", "Entreprise", "Document"));
        cbFilterStatut.setItems(FXCollections.observableArrayList("Tous", "en_attente", "validé", "rejeté"));
        cbFilterType.setValue("Tous");
        cbFilterStatut.setValue("Tous");

        loadAllEntries();
        applyFilters();

        cbFilterType.valueProperty().addListener((obs, o, n) -> applyFilters());
        cbFilterStatut.valueProperty().addListener((obs, o, n) -> applyFilters());
        dpFrom.valueProperty().addListener((obs, o, n) -> applyFilters());
        dpTo.valueProperty().addListener((obs, o, n) -> applyFilters());
    }

    private void loadAllEntries() {
        allEntries.clear();
        try {
            SessionManager session = SessionManager.getInstance();
            boolean isAdmin = session.isAdmin();
            int currentUserId = session.getUserId();

            List<Entreprise> enterprisesToLoad = new ArrayList<>();
            List<Document> documentsToLoad = new ArrayList<>();

            if (isAdmin) {
                enterprisesToLoad = entService.findAll();
                documentsToLoad = docService.findAll();
            } else {
                enterprisesToLoad = entService.findByOwnerId(currentUserId);
                List<Integer> myEntIds = enterprisesToLoad.stream().map(Entreprise::getId).collect(Collectors.toList());
                List<Document> allDocs = docService.findAll();
                documentsToLoad = allDocs.stream().filter(d -> myEntIds.contains(d.getEntrepriseId())).collect(Collectors.toList());
            }

            for (Entreprise e : enterprisesToLoad) {
                if (e.getDateCreation() == null) continue;
                HistoriqueEntry h = new HistoriqueEntry();
                h.date = e.getDateCreation();
                h.type = "Entreprise";
                h.nom = e.getNom();
                h.statut = e.getStatut() != null ? e.getStatut() : "en_attente";
                h.detail = e.getSecteur() != null ? e.getSecteur() : "";
                h.entityId = e.getId();
                allEntries.add(h);
            }
            for (Document d : documentsToLoad) {
                if (d.getDateUpload() == null) continue;
                HistoriqueEntry h = new HistoriqueEntry();
                h.date = d.getDateUpload();
                h.type = "Document";
                h.nom = d.getNom();
                h.statut = d.getStatut() != null ? d.getStatut() : "en_attente";
                h.detail = d.getType() != null ? d.getType() : "";
                h.entityId = d.getId();
                allEntries.add(h);
            }
            allEntries.sort(Comparator.comparing((HistoriqueEntry h) -> h.date).reversed());
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void applyFilters() {
        String typeFilter = cbFilterType.getValue();
        String statutFilter = cbFilterStatut.getValue();
        LocalDate from = dpFrom.getValue();
        LocalDate to = dpTo.getValue();

        List<HistoriqueEntry> filtered = allEntries.stream().filter(h -> {
            boolean typeOk = typeFilter == null || typeFilter.equals("Tous") || typeFilter.equals(h.type);
            boolean statutOk = statutFilter == null || statutFilter.equals("Tous") || statutFilter.equals(h.statut);
            LocalDate d = toLocalDate(h.date);
            boolean fromOk = from == null || !d.isBefore(from);
            boolean toOk = to == null || !d.isAfter(to);
            return typeOk && statutOk && fromOk && toOk;
        }).collect(Collectors.toList());

        buildTimeline(filtered);
        if (lblCount != null) lblCount.setText(filtered.size() + " événement(s)");
    }

    private static LocalDate toLocalDate(Date date) {
        if (date instanceof java.sql.Date) {
            return ((java.sql.Date) date).toLocalDate();
        }
        return date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
    }

    private void buildTimeline(List<HistoriqueEntry> entries) {
        vboxTimeline.getChildren().clear();
        if (entries.isEmpty()) {
            Label empty = new Label("Aucune activité trouvée.");
            empty.setStyle("-fx-text-fill: #475569; -fx-font-size: 13px; -fx-padding: 20;");
            vboxTimeline.getChildren().add(empty);
            return;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        for (HistoriqueEntry h : entries) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10, 16, 10, 16));
            row.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 8;" +
                    "-fx-border-color: rgba(255,255,255,0.05); -fx-border-radius: 8; -fx-border-width: 1;");

            Label icon = new Label(h.type.equals("Entreprise") ? "🏢" : "📄");
            icon.setStyle("-fx-font-size: 16px;");

            Label dot = new Label("●");
            String dotColor = "validé".equals(h.statut) ? "#10b981"
                    : "rejeté".equals(h.statut) ? "#ef4444" : "#f59e0b";
            dot.setStyle("-fx-text-fill: " + dotColor + "; -fx-font-size: 10px;");

            VBox info = new VBox(3);
            Label name = new Label(h.nom);
            name.setStyle("-fx-text-fill: #e2e8f0; -fx-font-weight: bold; -fx-font-size: 13px;");
            name.setMaxWidth(300);
            Label detail = new Label(h.type + " · " + h.detail + " · " + h.statut);
            detail.setStyle("-fx-text-fill: " + dotColor + "; -fx-font-size: 11px;");
            info.getChildren().addAll(name, detail);
            HBox.setHgrow(info, Priority.ALWAYS);

            Label dateLabel = new Label(sdf.format(h.date));
            dateLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 11px;");

            row.getChildren().addAll(icon, dot, info, dateLabel);
            vboxTimeline.getChildren().add(row);
        }
    }

    @FXML
    void handleReset() {
        cbFilterType.setValue("Tous");
        cbFilterStatut.setValue("Tous");
        dpFrom.setValue(null);
        dpTo.setValue(null);
    }

    @FXML
    void handleRefresh() {
        loadAllEntries();
        applyFilters();
    }
}
