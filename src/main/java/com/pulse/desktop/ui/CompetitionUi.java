package com.pulse.desktop.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

final class CompetitionUi {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private CompetitionUi() {
    }

    static HBox listRow(String leftText, String rightText) {
        HBox row = new HBox(10);
        row.getStyleClass().add("list-item");
        row.setAlignment(Pos.CENTER_LEFT);

        Label left = new Label(emptySafe(leftText));
        left.getStyleClass().add("list-item-title");
        left.setWrapText(true);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label right = new Label(emptySafe(rightText));
        right.getStyleClass().add("list-item-meta");

        row.getChildren().addAll(left, spacer, right);
        return row;
    }

    static HBox listRowWithActions(String leftText, String rightText, Button... buttons) {
        HBox row = new HBox(10);
        row.getStyleClass().add("list-item");
        row.setAlignment(Pos.CENTER_LEFT);

        Label left = new Label(emptySafe(leftText));
        left.getStyleClass().add("list-item-title");
        left.setWrapText(true);

        Label right = new Label(emptySafe(rightText));
        right.getStyleClass().add("list-item-meta");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox actions = new HBox(6);
        if (buttons != null) {
            for (Button button : buttons) {
                if (button == null) {
                    continue;
                }
                actions.getChildren().add(button);
            }
        }

        row.getChildren().addAll(left, right, spacer, actions);
        return row;
    }

    static Label badge(String text, String variant) {
        Label badge = new Label(emptySafe(text));
        badge.getStyleClass().addAll("badge", switch (variant) {
            case "success" -> "badge--success";
            case "warning" -> "badge--warning";
            case "danger" -> "badge--danger";
            default -> "badge--info";
        });
        return badge;
    }

    static Label emptyState(String text) {
        Label label = new Label(emptySafe(text));
        label.getStyleClass().add("empty-state");
        label.setWrapText(true);
        return label;
    }

    static VBox emptyStateBox(String text) {
        VBox box = new VBox();
        box.getChildren().add(emptyState(text));
        return box;
    }

    static String fmtDate(LocalDate value) {
        return value == null ? "-" : value.format(DATE);
    }

    static String fmtDateTime(LocalDateTime value) {
        return value == null ? "-" : value.format(DATE_TIME);
    }

    static String emptySafe(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value;
    }
}
