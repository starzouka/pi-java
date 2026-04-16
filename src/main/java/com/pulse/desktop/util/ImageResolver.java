package com.pulse.desktop.util;

import com.pulse.desktop.config.AppConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class ImageResolver {
    private ImageResolver() {
    }

    public static String toExternalForm(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return null;
        }

        String value = rawPath.trim();
        if (value.startsWith("http://") || value.startsWith("https://") || value.startsWith("file:/")) {
            return value;
        }

        String normalized = value.replace('\\', '/').trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        Path webRoot = AppConfig.webRootPath();
        Path[] candidates = new Path[]{
                webRoot.resolve(normalized).normalize(),
                webRoot.resolve("public").resolve(normalized).normalize(),
                Paths.get("").toAbsolutePath().resolve(normalized).normalize()
        };

        for (Path candidate : candidates) {
            if (Files.exists(candidate) && Files.isRegularFile(candidate)) {
                return candidate.toUri().toString();
            }
        }

        return null;
    }

    public static String toBackgroundStyle(String rawPath) {
        String external = toExternalForm(rawPath);
        if (external == null) {
            return "";
        }
        String escaped = external.replace("'", "\\'");
        return "-fx-background-image: url('" + escaped + "');";
    }
}
