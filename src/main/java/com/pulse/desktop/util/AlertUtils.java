package com.pulse.desktop.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;

public final class AlertUtils {
    private AlertUtils() {
    }

    public static void info(String title, String content) {
        show(Alert.AlertType.INFORMATION, title, content);
    }

    public static void warning(String title, String content) {
        show(Alert.AlertType.WARNING, title, content);
    }

    public static void error(String title, String content) {
        show(Alert.AlertType.ERROR, title, content);
    }

    public static boolean confirm(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private static void show(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
