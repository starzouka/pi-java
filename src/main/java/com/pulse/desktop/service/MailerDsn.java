package com.pulse.desktop.service;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public record MailerDsn(
        boolean enabled,
        String host,
        int port,
        String username,
        String password,
        boolean startTls,
        boolean ssl
) {
    public static MailerDsn parse(String dsn) {
        if (dsn == null) {
            return disabled();
        }
        String value = stripQuotes(dsn.trim());
        if (value.isBlank() || value.startsWith("null://")) {
            return disabled();
        }

        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException ex) {
            return disabled();
        }

        String scheme = lower(uri.getScheme());
        String host = uri.getHost();
        int port = uri.getPort();

        String username = "";
        String password = "";
        String userInfo = uri.getRawUserInfo();
        if (userInfo != null && !userInfo.isBlank()) {
            String[] chunks = userInfo.split(":", 2);
            username = decode(chunks[0]);
            if (chunks.length > 1) {
                password = decode(chunks[1]);
            }
        }

        Map<String, String> query = parseQuery(uri.getRawQuery());
        String encryption = lower(query.get("encryption"));

        boolean gmail = scheme.startsWith("gmail+smtp");
        if ((host == null || host.isBlank() || "default".equalsIgnoreCase(host)) && gmail) {
            host = "smtp.gmail.com";
        } else if (host == null || host.isBlank() || "default".equalsIgnoreCase(host)) {
            host = "localhost";
        }

        boolean ssl = "smtps".equals(scheme) || "ssl".equals(encryption);
        boolean tls = gmail || "tls".equals(encryption);

        if (port <= 0) {
            if (ssl) {
                port = 465;
            } else if (tls) {
                port = 587;
            } else {
                port = 25;
            }
        }

        return new MailerDsn(true, host, port, username, password, tls, ssl);
    }

    private static MailerDsn disabled() {
        return new MailerDsn(false, "", 25, "", "", false, false);
    }

    private static String stripQuotes(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static String lower(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> out = new HashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) {
            return out;
        }
        String[] pairs = rawQuery.split("&");
        for (String pair : pairs) {
            if (pair.isBlank()) {
                continue;
            }
            String[] chunks = pair.split("=", 2);
            String key = decode(chunks[0]).trim();
            if (key.isBlank()) {
                continue;
            }
            String value = chunks.length > 1 ? decode(chunks[1]).trim() : "";
            out.put(key, value);
        }
        return out;
    }
}
