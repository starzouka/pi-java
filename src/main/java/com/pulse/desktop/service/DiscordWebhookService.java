package com.pulse.desktop.service;

import com.pulse.desktop.config.AppConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class DiscordWebhookService {
    private final HttpClient httpClient;
    private final boolean enabled;
    private final String webhookUrl;
    private final String username;
    private final String avatarUrl;

    public DiscordWebhookService() {
        this.enabled = AppConfig.teamDiscordNotificationsEnabled();
        this.webhookUrl = safe(AppConfig.teamDiscordWebhookUrl());
        this.username = safe(AppConfig.teamDiscordWebhookUsername());
        this.avatarUrl = safe(AppConfig.teamDiscordWebhookAvatarUrl());
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(6))
                .build();
    }

    public boolean isConfigured() {
        return enabled && !webhookUrl.isBlank();
    }

    public void sendTeamEvent(String title, String body) throws IOException, InterruptedException {
        if (!isConfigured()) {
            return;
        }

        String content = "[" + safe(title) + "] " + safe(body);
        if (content.isBlank()) {
            return;
        }
        if (content.length() > 1850) {
            content = content.substring(0, 1850).trim() + "...";
        }

        StringBuilder payload = new StringBuilder(256 + content.length());
        payload.append("{");
        boolean needsComma = false;
        if (!username.isBlank()) {
            payload.append("\"username\":\"").append(escapeJson(username)).append("\"");
            needsComma = true;
        }
        if (!avatarUrl.isBlank()) {
            if (needsComma) {
                payload.append(',');
            }
            payload.append("\"avatar_url\":\"").append(escapeJson(avatarUrl)).append("\"");
            needsComma = true;
        }
        if (needsComma) {
            payload.append(',');
        }
        payload.append("\"content\":\"").append(escapeJson(content)).append("\"}");

        HttpRequest request = HttpRequest.newBuilder(URI.create(webhookUrl))
                .timeout(Duration.ofSeconds(12))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            String responseBody = response.body() == null ? "" : response.body().trim();
            throw new IOException("Discord webhook HTTP " + status + (responseBody.isBlank() ? "" : " - " + responseBody));
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String escapeJson(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}
