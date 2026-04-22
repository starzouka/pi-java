package com.pulse.desktop.util;

import javafx.scene.control.Alert;

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

    private static void show(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
