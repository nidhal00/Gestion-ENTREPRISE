package com.gestion.controller;

import com.gestion.util.SessionManager;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import java.io.IOException;

public class MainViewController {

    @FXML private StackPane contentArea;
    @FXML private Button btnHome, btnEntreprises, btnDocuments;
    @FXML private Button btnToggleRole;
    @FXML private Label lblPageTitle, lblPageSubtitle;
    @FXML private Label lblUserName, lblUserRole, lblRoleBadge;

    public void initialize() {
        refreshRoleDisplay();
        showHome();
    }

    @FXML void showHome() {
        loadView("view/HomeView.fxml", btnHome, "Tableau de Bord", "Vue d'ensemble du système");
    }

    @FXML void showEntreprises() {
        loadView("view/EntrepriseView.fxml", btnEntreprises, "Gestion des Entreprises",
                SessionManager.getInstance().isAdmin()
                        ? "Administration et validation des clients"
                        : "Vos informations d'entreprise");
    }

    @FXML void showDocuments() {
        loadView("view/DocumentView.fxml", btnDocuments, "Centre de Documents",
                SessionManager.getInstance().isAdmin()
                        ? "Validation et archivage de tous les documents"
                        : "Vos pièces justificatives déposées");
    }

    @FXML void handleToggleRole() {
        SessionManager.getInstance().toggleRole();
        refreshRoleDisplay();
        showHome();
    }

    private void refreshRoleDisplay() {
        SessionManager session = SessionManager.getInstance();
        boolean isAdmin = session.isAdmin();

        if (lblUserName  != null) lblUserName.setText(session.getName());
        if (lblUserRole  != null) lblUserRole.setText(isAdmin ? "Administrateur" : "Entreprise / User");
        if (btnToggleRole != null) btnToggleRole.setText("🔄  Rôle actuel : " + session.getRole());

        if (lblRoleBadge != null) {
            if (isAdmin) {
                lblRoleBadge.setText("ADMIN");
                lblRoleBadge.setStyle("-fx-background-color: rgba(99,102,241,0.15); -fx-text-fill: #818cf8; -fx-font-weight: bold; -fx-font-size: 10px; -fx-background-radius: 6; -fx-padding: 4 10; -fx-border-color: rgba(99,102,241,0.3); -fx-border-radius: 6; -fx-border-width: 1;");
            } else {
                lblRoleBadge.setText("USER");
                lblRoleBadge.setStyle("-fx-background-color: rgba(16,185,129,0.15); -fx-text-fill: #34d399; -fx-font-weight: bold; -fx-font-size: 10px; -fx-background-radius: 6; -fx-padding: 4 10; -fx-border-color: rgba(16,185,129,0.3); -fx-border-radius: 6; -fx-border-width: 1;");
            }
        }
    }

    private void loadView(String fxmlPath, Button activeBtn, String title, String subtitle) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource("/com/gestion/" + fxmlPath));

            if (lblPageTitle    != null) lblPageTitle.setText(title);
            if (lblPageSubtitle != null) lblPageSubtitle.setText(subtitle);

            view.setOpacity(0);
            contentArea.getChildren().setAll(view);

            FadeTransition ft = new FadeTransition(Duration.millis(350), view);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.play();

            updateActiveButton(activeBtn);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateActiveButton(Button active) {
        for (Button b : new Button[]{btnHome, btnEntreprises, btnDocuments}) {
            if (b != null) b.getStyleClass().remove("nav-button-active");
        }
        if (active != null && !active.getStyleClass().contains("nav-button-active")) {
            active.getStyleClass().add("nav-button-active");
        }
    }
}
