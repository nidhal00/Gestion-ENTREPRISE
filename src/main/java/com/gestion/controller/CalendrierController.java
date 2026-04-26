package com.gestion.controller;

import com.gestion.entity.Document;
import com.gestion.entity.Entreprise;
import com.gestion.service.DocumentService;
import com.gestion.service.EntrepriseService;
import com.gestion.util.SessionManager;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class CalendrierController {

    @FXML private Label      lblMonthYear;
    @FXML private GridPane   gridCalendar;

    private YearMonth currentYearMonth;
    private final EntrepriseService entService = new EntrepriseService();
    private final DocumentService   docService = new DocumentService();

    public void initialize() {
        currentYearMonth = YearMonth.now();
        drawCalendar();
    }

    private void drawCalendar() {
        lblMonthYear.setText(translateMonth(currentYearMonth.getMonthValue()) + " " + currentYearMonth.getYear());
        gridCalendar.getChildren().clear();

        LocalDate firstOfMonth = currentYearMonth.atDay(1);
        int offset = firstOfMonth.getDayOfWeek().getValue() - 1;

        try {
            SessionManager session = SessionManager.getInstance();
            List<Entreprise> enterprises;
            List<Document>   documents;

            if (session.isAdmin()) {
                enterprises = entService.findInMonth(currentYearMonth.getYear(), currentYearMonth.getMonthValue());
                documents   = docService.findInMonth(currentYearMonth.getYear(), currentYearMonth.getMonthValue());
            } else {
                // User: show only their own entreprise and related docs
                List<Entreprise> myEnts = entService.findByOwnerId(session.getUserId());
                if (myEnts.isEmpty()) {
                    enterprises = Collections.emptyList();
                    documents   = Collections.emptyList();
                } else {
                    int entId = myEnts.get(0).getId();
                    // Filter to only this month
                    enterprises = entService.findInMonth(currentYearMonth.getYear(), currentYearMonth.getMonthValue())
                        .stream().filter(e -> e.getId() == entId).collect(Collectors.toList());
                    // All docs for user's company this month
                    List<Document> allDocs = docService.findInMonth(currentYearMonth.getYear(), currentYearMonth.getMonthValue());
                    documents = allDocs.stream().filter(d -> d.getEntrepriseId() == entId).collect(Collectors.toList());
                }
            }

            for (int i = 0; i < 42; i++) {
                int dayNum = i - offset + 1;
                VBox cell = new VBox(5);
                cell.setAlignment(Pos.TOP_LEFT);
                cell.setPadding(new javafx.geometry.Insets(8));
                cell.getStyleClass().add("calendar-cell-pane");
                cell.setMinWidth(120);
                cell.setMinHeight(100);

                if (dayNum > 0 && dayNum <= currentYearMonth.lengthOfMonth()) {
                    Label lblDay = new Label(String.valueOf(dayNum));
                    lblDay.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
                    cell.getChildren().add(lblDay);

                    final int d = dayNum;
                    List<Entreprise> dayEnts = enterprises.stream()
                        .filter(e -> toLocalDate(e.getDateCreation()).getDayOfMonth() == d)
                        .collect(Collectors.toList());

                    List<Document> dayDocs = documents.stream()
                        .filter(doc -> toLocalDate(doc.getDateUpload()).getDayOfMonth() == d)
                        .collect(Collectors.toList());

                    if (!dayEnts.isEmpty()) {
                        Label l = new Label("🏢 " + dayEnts.size() + " Ent.");
                        l.setStyle("-fx-background-color: rgba(99,102,241,0.15); -fx-text-fill: #818cf8; -fx-padding: 2 6; -fx-background-radius: 4; -fx-font-size: 10px; -fx-font-weight: bold;");
                        cell.getChildren().add(l);
                    }
                    if (!dayDocs.isEmpty()) {
                        Label l = new Label("📄 " + dayDocs.size() + " Doc.");
                        l.setStyle("-fx-background-color: rgba(16,185,129,0.15); -fx-text-fill: #34d399; -fx-padding: 2 6; -fx-background-radius: 4; -fx-font-size: 10px; -fx-font-weight: bold;");
                        cell.getChildren().add(l);
                    }

                    if (LocalDate.now().equals(currentYearMonth.atDay(dayNum))) {
                        cell.setStyle("-fx-background-color: rgba(99,102,241,0.1); -fx-border-color: #6366f1; -fx-border-width: 0 0 0 4; -fx-background-radius: 0 8 8 0;");
                    } else {
                        cell.setStyle("-fx-background-color: rgba(255,255,255,0.02); -fx-background-radius: 8; -fx-border-color: rgba(255,255,255,0.05); -fx-border-width: 1;");
                    }

                    // Interaction: Click to see details
                    cell.setCursor(javafx.scene.Cursor.HAND);
                    cell.setOnMouseClicked(e -> showDayDetails(currentYearMonth.atDay(d), dayEnts, dayDocs));
                    
                } else {
                    cell.setStyle("-fx-opacity: 0.1;");
                }

                gridCalendar.add(cell, i % 7, i / 7);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void showDayDetails(LocalDate date, List<Entreprise> ents, List<Document> docs) {
        if (ents.isEmpty() && docs.isEmpty()) return;

        VBox content = new VBox(15);
        content.setPadding(new javafx.geometry.Insets(20));
        content.setStyle("-fx-background-color: #0f172a;");
        content.setMinWidth(350);

        Label title = new Label("Détails du " + date.format(java.time.format.DateTimeFormatter.ofPattern("dd MMMM yyyy")));
        title.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        content.getChildren().add(title);

        if (!ents.isEmpty()) {
            VBox vEnts = new VBox(5);
            Label h = new Label("🏢 Entreprises créées (" + ents.size() + ")");
            h.setStyle("-fx-text-fill: #818cf8; -fx-font-weight: bold; -fx-font-size: 13px;");
            vEnts.getChildren().add(h);
            for (Entreprise e : ents) {
                Label l = new Label(" • " + e.getNom() + " (" + e.getSecteur() + ")");
                l.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 12px;");
                vEnts.getChildren().add(l);
            }
            content.getChildren().add(vEnts);
        }

        if (!docs.isEmpty()) {
            VBox vDocs = new VBox(5);
            Label h = new Label("📄 Documents déposés (" + docs.size() + ")");
            h.setStyle("-fx-text-fill: #34d399; -fx-font-weight: bold; -fx-font-size: 13px;");
            vDocs.getChildren().add(h);
            for (Document d : docs) {
                String status = (d.getStatut() != null) ? d.getStatut() : "en_attente";
                Label l = new Label(" • " + d.getNom() + " [" + status + "]");
                String color = "validé".equals(status) ? "#10b981" : "rejeté".equals(status) ? "#ef4444" : "#f59e0b";
                l.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px;");
                vDocs.getChildren().add(l);
            }
            content.getChildren().add(vDocs);
        }

        javafx.scene.Scene scene = new javafx.scene.Scene(content);
        javafx.stage.Stage stage = new javafx.stage.Stage();
        stage.setTitle("MindAudit - " + date);
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        stage.setScene(scene);
        stage.show();
    }

    @FXML void handlePrevMonth() { currentYearMonth = currentYearMonth.minusMonths(1); drawCalendar(); }
    @FXML void handleNextMonth() { currentYearMonth = currentYearMonth.plusMonths(1); drawCalendar(); }

    private LocalDate toLocalDate(java.util.Date date) {
        if (date == null) return LocalDate.MIN;
        if (date instanceof java.sql.Date) return ((java.sql.Date) date).toLocalDate();
        return date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    private String translateMonth(int m) {
        return switch (m) {
            case 1 -> "Janvier"; case 2 -> "Février"; case 3 -> "Mars";
            case 4 -> "Avril"; case 5 -> "Mai"; case 6 -> "Juin";
            case 7 -> "Juillet"; case 8 -> "Août"; case 9 -> "Septembre";
            case 10 -> "Octobre"; case 11 -> "Novembre"; case 12 -> "Décembre";
            default -> "";
        };
    }

    private static class HistEntry {
        java.util.Date date; String icon, name, description, color;
        HistEntry(java.util.Date date, String icon, String name, String description, String color) {
            this.date = date; this.icon = icon; this.name = name;
            this.description = description; this.color = color;
        }
    }
}
