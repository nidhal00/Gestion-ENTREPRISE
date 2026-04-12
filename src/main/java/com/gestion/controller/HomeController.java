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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HomeController {

    @FXML private Label lblTotalEntreprises;
    @FXML private Label lblTotalDocuments;
    @FXML private Label lblGlobalCompliance;
    @FXML private PieChart chartTaille;
    @FXML private BarChart<String, Number> chartDocs;

    private EntrepriseService entService = new EntrepriseService();
    private DocumentService docService = new DocumentService();

    private static final List<String> CORE_CATEGORIES = Arrays.asList("RH", "fiscal", "financier", "ISO");

    public void initialize() {
        loadData();
    }

    private void loadData() {
        try {
            SessionManager session = SessionManager.getInstance();
            List<Entreprise> enterprises;
            List<Document> documents;

            if (session.isAdmin()) {
                enterprises = entService.findAll();
                documents = docService.findAll();
            } else {
                enterprises = entService.findByOwnerId(session.getUserId());
                if (!enterprises.isEmpty()) {
                    documents = docService.findByEntrepriseId(enterprises.get(0).getId());
                } else {
                    documents = java.util.Collections.emptyList();
                }
            }

            animateLabel(lblTotalEntreprises, enterprises.size());
            animateLabel(lblTotalDocuments, documents.size());

            double complianceScore = calculateCompliance(enterprises);
            lblGlobalCompliance.setText(String.format("%.1f %%", complianceScore));
            animateLabelFade(lblGlobalCompliance);

            setupPieChart(enterprises, session.isAdmin());
            setupBarChart(documents);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setupPieChart(List<Entreprise> enterprises, boolean isAdmin) {
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        if (isAdmin) {
            Map<String, Long> sectorGroups = enterprises.stream()
                    .collect(Collectors.groupingBy(
                            e -> (e.getSecteur() != null && !e.getSecteur().isEmpty()) ? e.getSecteur() : "Non défini",
                            Collectors.counting()));
            sectorGroups.forEach((sector, count) -> pieData.add(new PieChart.Data(sector + " (" + count + ")", count)));
        } else {
            if (!enterprises.isEmpty()) {
                Entreprise e = enterprises.get(0);
                pieData.add(new PieChart.Data(e.getNom(), 100));
            }
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
        typeGroups.forEach((type, count) -> series.getData().add(new XYChart.Data<>(type, count)));
        chartDocs.getData().clear();
        chartDocs.getData().add(series);
        chartDocs.setAnimated(true);
    }

    private double calculateCompliance(List<Entreprise> enterprises) throws SQLException {
        if (enterprises.isEmpty()) return 0.0;
        
        Map<Integer, List<String>> categoriesMap = docService.getCategoriesPerEntreprise();
        double totalScore = 0;
        
        for (Entreprise ent : enterprises) {
            List<String> types = categoriesMap.get(ent.getId());
            if (types == null || types.isEmpty()) continue;

            long uniqueCoreCount = types.stream()
                    .distinct()
                    .filter(CORE_CATEGORIES::contains)
                    .count();
            
            totalScore += (uniqueCoreCount / (double) CORE_CATEGORIES.size()) * 100;
        }
        
        return totalScore / enterprises.size();
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
