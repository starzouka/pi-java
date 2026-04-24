package com.pulse.desktop.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

public final class QrCodeService {
    private QrCodeService() {
    }

    public static Image generatePngImage(String content, int sizePx) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Contenu QR code vide.");
        }

        int size = Math.max(128, sizePx);
        QRCodeWriter writer = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
        hints.put(EncodeHintType.MARGIN, 1);

        BitMatrix matrix;
        try {
            matrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints);
        } catch (WriterException ex) {
            throw new IllegalStateException("Impossible de generer le QR code.", ex);
        }

        int width = matrix.getWidth();
        int height = matrix.getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        int onColor = 0xFF0B1324;
        int offColor = 0xFFFFFFFF;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, matrix.get(x, y) ? onColor : offColor);
            }
        }

        return SwingFXUtils.toFXImage(image, null);
    }
}

