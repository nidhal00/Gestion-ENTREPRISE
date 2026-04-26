package com.gestion.controller;

import com.gestion.entity.Document;
import com.gestion.entity.Entreprise;
import com.gestion.service.DocumentService;
import com.gestion.service.EntrepriseService;
import com.gestion.util.SessionManager;
import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class HomeController {

    @FXML private Label lblTotalEntreprises;
    @FXML private Label lblTotalDocuments;
    @FXML private Label lblGlobalCompliance;
    @FXML private Label lblPendingDocs;
    @FXML private Label lblStatutEntreprises;
    @FXML private Label lblStatutDocuments;
    @FXML private Label lblTopSecteur;
    @FXML private Label lblTauxValidation;

    @FXML private PieChart                chartSecteur;
    @FXML private BarChart<String, Number> chartDocs;
    @FXML private BarChart<String, Number> chartStatutEnt;
    @FXML private LineChart<String, Number> chartEvolution;
    @FXML private VBox                     vboxTopSecteurs;

    private final EntrepriseService entService = new EntrepriseService();
    private final DocumentService   docService = new DocumentService();

    public void initialize() {
        loadData();
    }

    private void loadData() {
        try {
            SessionManager     session     = SessionManager.getInstance();
            List<Entreprise>   enterprises;
            List<Document>     documents;

            if (session.isAdmin()) {
                enterprises = entService.findAll();
                documents   = docService.findAll();
            } else {
                enterprises = entService.findByOwnerId(session.getUserId());
                documents   = enterprises.isEmpty()
                    ? Collections.emptyList()
                    : docService.findByEntrepriseId(enterprises.get(0).getId());
            }

            // ── KPI Cards ─────────────────────────────────────────────────────
            animateLabel(lblTotalEntreprises, enterprises.size());
            animateLabel(lblTotalDocuments,   documents.size());

            long docPending  = documents.stream().filter(d -> "en_attente".equals(d.getStatut())).count();
            long docValides  = documents.stream().filter(d -> "validé".equals(d.getStatut())).count();
            long docRejetes  = documents.stream().filter(d -> "rejeté".equals(d.getStatut())).count();
            animateLabel(lblPendingDocs, (int) docPending);

            long entValides  = enterprises.stream().filter(e -> "validé".equals(e.getStatut())).count();
            long entPending  = enterprises.stream().filter(e -> "en_attente".equals(e.getStatut())).count();
            long entRejetes  = enterprises.stream().filter(e -> "rejeté".equals(e.getStatut())).count();

            if (lblStatutEntreprises != null)
                lblStatutEntreprises.setText("✅ " + entValides + "  ⏳ " + entPending + "  ❌ " + entRejetes);
            if (lblStatutDocuments != null)
                lblStatutDocuments.setText("✅ " + docValides + "  ⏳ " + docPending + "  ❌ " + docRejetes);

            // Taux de validation documents
            double tauxValidation = documents.isEmpty() ? 0 : (docValides * 100.0 / documents.size());
            if (lblTauxValidation != null)
                lblTauxValidation.setText(String.format("Taux de validation : %.0f %%", tauxValidation));

            // Conformité moyenne + secteur dominant
            double totalScore = 0;
            Map<String, Integer> secteurCount = new HashMap<>();
            for (Entreprise ent : enterprises) {
                totalScore += docService.getComplianceScore(ent.getId());
                if (ent.getSecteur() != null && !ent.getSecteur().isEmpty()) {
                    secteurCount.merge(ent.getSecteur(), 1, Integer::sum);
                }
            }
            double avg = enterprises.isEmpty() ? 0 : totalScore / enterprises.size();
            lblGlobalCompliance.setText(String.format("%.1f %%", avg));
            animateLabelFade(lblGlobalCompliance);

            // Top secteur dominant
            String topSecteur = secteurCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("—");
            if (lblTopSecteur != null)
                lblTopSecteur.setText("Secteur dominant : " + topSecteur);

            // ── Charts ────────────────────────────────────────────────────────
            setupPieChart(enterprises, session.isAdmin(), secteurCount);
            setupBarChartDocs(documents);
            setupBarChartStatutEnt(entValides, entPending, entRejetes);
            setupLineChart();
            setupTopSecteursList(secteurCount);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setupPieChart(List<Entreprise> enterprises, boolean isAdmin, Map<String, Integer> secteurCount) {
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        if (isAdmin && !secteurCount.isEmpty()) {
            // Sort by count descending, show top 8
            secteurCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(8)
                .forEach(entry -> pieData.add(new PieChart.Data(
                    entry.getKey() + " (" + entry.getValue() + ")", entry.getValue())));
        } else if (!enterprises.isEmpty()) {
            pieData.add(new PieChart.Data(enterprises.get(0).getNom(), 100));
        }
        chartSecteur.setAnimated(false);
        chartSecteur.setData(pieData);
        chartSecteur.setLabelsVisible(true);
    }

    private void setupBarChartDocs(List<Document> documents) {
        Map<String, Long> typeGroups = documents.stream()
            .collect(Collectors.groupingBy(
                d -> (d.getType() != null && !d.getType().isEmpty()) ? d.getType() : "Autre",
                Collectors.counting()));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Documents");
        typeGroups.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .forEach(e -> series.getData().add(new XYChart.Data<>(e.getKey(), e.getValue())));

        chartDocs.setAnimated(false);
        chartDocs.getData().clear();
        chartDocs.getData().add(series);
    }

    private void setupBarChartStatutEnt(long valides, long attente, long rejetes) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Statut");
        series.getData().add(new XYChart.Data<>("Validées", valides));
        series.getData().add(new XYChart.Data<>("En attente", attente));
        series.getData().add(new XYChart.Data<>("Rejetées", rejetes));

        chartStatutEnt.setAnimated(false);
        chartStatutEnt.getData().clear();
        chartStatutEnt.getData().add(series);
    }

    private void setupLineChart() throws SQLException {
        Map<String, Integer> monthData = docService.countByMonth();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Dépôts Mensuels");
        monthData.forEach((month, count) -> series.getData().add(new XYChart.Data<>(month, count)));

        chartEvolution.setAnimated(false);
        chartEvolution.getData().clear();
        chartEvolution.getData().add(series);
    }

    private void setupTopSecteursList(Map<String, Integer> secteurCount) {
        if (vboxTopSecteurs == null) return;
        vboxTopSecteurs.getChildren().clear();

        List<Map.Entry<String, Integer>> sorted = secteurCount.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(6)
            .collect(Collectors.toList());

        int max = sorted.isEmpty() ? 1 : sorted.get(0).getValue();
        String[] colors = {"#6366f1", "#10b981", "#f59e0b", "#ef4444", "#8b5cf6", "#06b6d4"};

        for (int i = 0; i < sorted.size(); i++) {
            Map.Entry<String, Integer> entry = sorted.get(i);
            String color = colors[i % colors.length];
            double pct = (double) entry.getValue() / max;

            VBox row = new VBox(4);
            HBox nameRow = new HBox(8);
            nameRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            Label rank = new Label((i + 1) + ".");
            rank.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-font-size: 12px; -fx-min-width: 20px;");

            Label name = new Label(entry.getKey());
            name.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 11px;");
            name.setMaxWidth(160);
            name.setWrapText(false);
            javafx.scene.layout.HBox.setHgrow(name, javafx.scene.layout.Priority.ALWAYS);

            Label count = new Label(String.valueOf(entry.getValue()));
            count.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-font-size: 12px;");

            nameRow.getChildren().addAll(rank, name, count);

            // Progress bar
            Pane barBg = new Pane();
            barBg.setPrefHeight(4);
            barBg.setStyle("-fx-background-color: rgba(255,255,255,0.07); -fx-background-radius: 2;");

            Pane barFill = new Pane();
            barFill.setPrefHeight(4);
            barFill.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 2;");

            barBg.getChildren().add(barFill);
            barBg.widthProperty().addListener((obs, ov, nv) ->
                barFill.setPrefWidth(nv.doubleValue() * pct));

            row.getChildren().addAll(nameRow, barBg);
            vboxTopSecteurs.getChildren().add(row);
        }
    }

    private void animateLabel(Label label, int target) {
        label.setText(String.valueOf(target));
        animateLabelFade(label);
    }

    private void animateLabelFade(Label label) {
        FadeTransition ft = new FadeTransition(Duration.millis(600), label);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }
}
