package com.pulse.desktop.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SymfonyEnv {
    private static final Map<String, String> VALUES = loadValues();

    private SymfonyEnv() {
    }

    public static String get(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        return VALUES.get(key);
    }

    private static Map<String, String> loadValues() {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        Path root = AppConfig.webRootPath();

        readFile(root.resolve(".env"), map);
        readFile(root.resolve(".env.local"), map);

        String appEnv = map.getOrDefault("APP_ENV", "dev");
        if (!appEnv.isBlank()) {
            readFile(root.resolve(".env." + appEnv), map);
            readFile(root.resolve(".env." + appEnv + ".local"), map);
        }

        for (Map.Entry<String, String> entry : map.entrySet()) {
            String envValue = System.getenv(entry.getKey());
            if (envValue != null && !envValue.isBlank()) {
                entry.setValue(envValue.trim());
            }
        }

        return map;
    }

    private static void readFile(Path path, Map<String, String> destination) {
        if (path == null || !Files.isRegularFile(path)) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            for (String rawLine : lines) {
                parseLine(rawLine, destination);
            }
        } catch (IOException ignored) {
            // Ignore unreadable optional env files.
        }
    }

    private static void parseLine(String rawLine, Map<String, String> destination) {
        if (rawLine == null) {
            return;
        }
        String line = rawLine.trim();
        if (line.isBlank() || line.startsWith("#")) {
            return;
        }
        if (line.startsWith("export ")) {
            line = line.substring("export ".length()).trim();
        }

        int equals = line.indexOf('=');
        if (equals <= 0) {
            return;
        }

        String key = line.substring(0, equals).trim();
        String value = line.substring(equals + 1).trim();
        if (key.isBlank()) {
            return;
        }

        destination.put(key, decodeValue(value));
    }

    private static String decodeValue(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return unescapeDoubleQuoted(trimmed.substring(1, trimmed.length() - 1));
        }
        if (trimmed.length() >= 2 && trimmed.startsWith("'") && trimmed.endsWith("'")) {
            return trimmed.substring(1, trimmed.length() - 1);
        }

        int commentIndex = trimmed.indexOf(" #");
        if (commentIndex >= 0) {
            trimmed = trimmed.substring(0, commentIndex).trim();
        }

        return trimmed;
    }

    private static String unescapeDoubleQuoted(String value) {
        StringBuilder out = new StringBuilder(value.length());
        boolean escaping = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (escaping) {
                out.append(switch (c) {
                    case 'n' -> '\n';
                    case 'r' -> '\r';
                    case 't' -> '\t';
                    case '"' -> '"';
                    case '\\' -> '\\';
                    default -> c;
                });
                escaping = false;
                continue;
            }
            if (c == '\\') {
                escaping = true;
            } else {
                out.append(c);
            }
        }
        if (escaping) {
            out.append('\\');
        }
        return out.toString();
    }
}
