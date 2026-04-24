package com.pulse.desktop.util;

import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

public final class ClipboardUtils {
    private ClipboardUtils() {
    }

    public static boolean copyToClipboard(String value) {
        if (value == null) {
            return false;
        }
        try {
            ClipboardContent content = new ClipboardContent();
            content.putString(value);
            return Clipboard.getSystemClipboard().setContent(content);
        } catch (Exception ignored) {
            return false;
        }
    }
}

