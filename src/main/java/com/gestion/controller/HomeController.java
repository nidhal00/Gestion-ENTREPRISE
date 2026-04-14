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
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.util.Duration;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HomeController {

    @FXML private Label   lblTotalEntreprises;
    @FXML private Label   lblTotalDocuments;
    @FXML private Label   lblGlobalCompliance;
    @FXML private PieChart                chartTaille;
    @FXML private BarChart<String, Number> chartDocs;

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

            animateLabel(lblTotalEntreprises, enterprises.size());
            animateLabel(lblTotalDocuments,   documents.size());

            // Compliance moyenne via SQL
            double total = 0;
            for (Entreprise ent : enterprises) {
                total += docService.getComplianceScore(ent.getId());
            }
            double avg = enterprises.isEmpty() ? 0 : total / enterprises.size();
            lblGlobalCompliance.setText(String.format("%.1f %%", avg));
            animateLabelFade(lblGlobalCompliance);

            setupPieChart(enterprises, session.isAdmin());
            setupBarChart(documents);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ── Charts ─────────────────────────────────────────────────
    private void setupPieChart(List<Entreprise> enterprises, boolean isAdmin) {
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();

        if (isAdmin) {
            Map<String, Long> sectorGroups = enterprises.stream()
                .collect(Collectors.groupingBy(
                    e -> (e.getSecteur() != null && !e.getSecteur().isEmpty())
                         ? e.getSecteur() : "Non défini",
                    Collectors.counting()));
            sectorGroups.forEach((sector, count) ->
                pieData.add(new PieChart.Data(sector + " (" + count + ")", count)));
        } else if (!enterprises.isEmpty()) {
            Entreprise e = enterprises.get(0);
            pieData.add(new PieChart.Data(e.getNom(), 100));
        }

        chartTaille.setData(pieData);
        chartTaille.setAnimated(true);
    }

    private void setupBarChart(List<Document> documents) {
        Map<String, Long> typeGroups = documents.stream()
            .collect(Collectors.groupingBy(
                d -> (d.getType() != null && !d.getType().isEmpty()) ? d.getType() : "Autre",
                Collectors.counting()));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Documents");
        typeGroups.forEach((type, count) ->
            series.getData().add(new XYChart.Data<>(type, count)));

        chartDocs.getData().clear();
        chartDocs.getData().add(series);
        chartDocs.setAnimated(true);
    }

    // ── Animation helpers ─────────────────────────────────────
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
