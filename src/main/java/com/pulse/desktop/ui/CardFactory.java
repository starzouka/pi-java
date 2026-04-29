package com.pulse.desktop.ui;

import com.pulse.desktop.model.SimpleCard;
import com.pulse.desktop.service.Navigator;
import com.pulse.desktop.util.ImageResolver;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public final class CardFactory {
    private static final String DEFAULT_IMAGE = "assets/template_fo/img/ll.png";

    private CardFactory() {
    }

    public static VBox create(SimpleCard card) {
        String variant = normalizeVariant(card.cardVariant());

        VBox cardBox = new VBox();
        cardBox.getStyleClass().addAll("card", "card--" + variant);
        cardBox.setPrefWidth(305);
        cardBox.setMinWidth(280);

        Region media = new Region();
        media.getStyleClass().add("card__media");
        media.setPrefHeight(mediaHeight(variant));

        String style = ImageResolver.toBackgroundStyle(firstNonBlank(card.imagePath(), DEFAULT_IMAGE));
        media.setStyle(style);

        HBox chips = new HBox(6);
        chips.getStyleClass().add("card__chips");

        Label badgeLeft = new Label(safe(card.badgeLeft()));
        badgeLeft.getStyleClass().addAll("chip", chipClass(variant, true));
        Label badgeRight = new Label(safe(card.badgeRight()));
        badgeRight.getStyleClass().addAll("chip", chipClass(variant, false));
        chips.getChildren().addAll(badgeLeft, badgeRight);

        VBox body = new VBox(8);
        body.getStyleClass().add("card__body");

        Label title = new Label(safe(card.title()));
        title.getStyleClass().add("card__title");
        title.setWrapText(true);

        Label subtitle = new Label(safe(card.subtitle()));
        subtitle.getStyleClass().add("card__desc");
        subtitle.setWrapText(true);

         Button openButton = new Button("Voir detail");
         openButton.getStyleClass().addAll("btn-ghost", "btn-compact");
         openButton.setOnAction(event -> Navigator.goTo(card.targetRoute()));

        HBox actions = new HBox(openButton);
        actions.getStyleClass().add("card__actions");

        HBox.setHgrow(openButton, Priority.NEVER);
        body.getChildren().addAll(title, subtitle, actions);

        StackPane mediaWrapper = new StackPane();
        mediaWrapper.setPadding(new Insets(0));
        mediaWrapper.getChildren().addAll(media, chips);
        StackPane.setAlignment(chips, javafx.geometry.Pos.TOP_LEFT);

        cardBox.getChildren().addAll(mediaWrapper, body);
        return cardBox;
    }

    private static String normalizeVariant(String variant) {
        if (variant == null || variant.isBlank()) {
            return "generic";
        }
        return switch (variant.trim().toLowerCase()) {
            case "tournament", "product", "game", "team", "member", "social" -> variant.trim().toLowerCase();
            default -> "generic";
        };
    }

    private static String chipClass(String variant, boolean primary) {
        return switch (variant) {
            case "tournament" -> primary ? "chip--status" : "chip--format";
            case "product" -> primary ? "chip--price" : "chip--team";
            case "game" -> primary ? "chip--category" : "chip--meta";
            case "team" -> primary ? "chip--region" : "chip--meta";
            case "member", "social" -> primary ? "chip--role" : "chip--meta";
            default -> "chip--meta";
        };
    }

    private static double mediaHeight(String variant) {
        return switch (variant) {
            case "tournament" -> 172;
            case "product", "game" -> 162;
            case "team", "member", "social" -> 152;
            default -> 152;
        };
    }

    private static String safe(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value;
    }

    private static String firstNonBlank(String first, String fallback) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return fallback;
    }
}
