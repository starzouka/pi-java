package com.pulse.desktop.util;

import com.pulse.desktop.service.QrCodeService;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;

public final class QrCodeDialog {
    private QrCodeDialog() {
    }

    public static void show(String title, String header, String content) {
        Image qr = QrCodeService.generatePngImage(content, 280);
        ImageView view = new ImageView(qr);
        view.setFitWidth(280);
        view.setFitHeight(280);
        view.setPreserveRatio(true);

        ClipboardUtils.copyToClipboard(content);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title == null ? "QR Code" : title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.setGraphic(view);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.showAndWait();
    }
}

