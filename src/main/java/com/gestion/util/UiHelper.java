package com.gestion.util;

import javafx.scene.control.Alert;
import javafx.scene.control.Control;
import javafx.scene.control.Label;

public class UiHelper {

    public static void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static void showError(Label label, String message, Control field) {
        label.setText(message);
        label.setVisible(true);
        if (field != null && !field.getStyleClass().contains("error-field")) {
            field.getStyleClass().add("error-field");
        }
    }

    public static void hideError(Label label, Control field) {
        if (label != null) label.setVisible(false);
        if (field != null) field.getStyleClass().remove("error-field");
    }

    public static void resetFieldStyles(Control... fields) {
        for (Control field : fields) {
            if (field != null) field.getStyleClass().remove("error-field");
        }
    }
}
