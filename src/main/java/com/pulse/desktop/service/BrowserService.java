package com.pulse.desktop.service;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public final class BrowserService {
    private BrowserService() {
    }

    public static void openUrl(String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        if (!Desktop.isDesktopSupported()) {
            return;
        }
        try {
            Desktop.getDesktop().browse(new URI(url.trim()));
        } catch (IOException | URISyntaxException ignored) {
            // Ignore when desktop shell cannot open a browser.
        }
    }
}
