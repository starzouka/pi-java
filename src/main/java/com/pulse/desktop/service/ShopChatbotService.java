package com.pulse.desktop.service;

import com.pulse.desktop.model.ChatMessage;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShopChatbotService {
    private static final Pattern CONTENT_PATTERN = Pattern.compile("\\\"content\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"");

    private final OpenRouterConfig config;
    private final HttpClient httpClient;

    public ShopChatbotService() {
        this(OpenRouterConfig.load(), HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build());
    }

    ShopChatbotService(OpenRouterConfig config, HttpClient httpClient) {
        this.config = config;
        this.httpClient = httpClient;
    }

    public boolean isConfigured() {
        return config.enabled() && !config.apiKey().isBlank();
    }

    public String reply(List<ChatMessage> history, String userMessage) throws IOException, InterruptedException {
        if (!isConfigured()) {
            throw new IllegalStateException("OpenRouter non configure.");
        }
        String message = safe(userMessage);
        if (message.isBlank()) {
            return "Veuillez écrire une question sur la boutique.";
        }

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", systemPrompt()));
        if (history != null && !history.isEmpty()) {
            int start = Math.max(0, history.size() - 8);
            for (int i = start; i < history.size(); i++) {
                ChatMessage entry = history.get(i);
                if (entry == null || safe(entry.content()).isBlank()) {
                    continue;
                }
                String role = normalizeRole(entry.role());
                if ("system".equals(role)) {
                    continue;
                }
                messages.add(new ChatMessage(role, entry.content()));
            }
        }
        messages.add(new ChatMessage("user", message));

        String body = buildRequestBody(messages);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.baseUrl()))
                .timeout(Duration.ofSeconds(config.timeoutSeconds()))
                .header("Authorization", "Bearer " + config.apiKey())
                .header("Content-Type", "application/json")
                .header("HTTP-Referer", config.siteUrl())
                .header("X-Title", config.appName())
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("OpenRouter HTTP " + response.statusCode() + ": " + trimForError(response.body()));
        }

        String content = extractAssistantContent(response.body());
        if (content == null || content.isBlank()) {
            throw new IOException("Réponse OpenRouter vide.");
        }
        return content.trim();
    }

    private String systemPrompt() {
        return "Tu es l'assistant boutique de PULSE. "
                + "Tu aides uniquement les clients sur les produits, stocks, paniers, commandes, livraisons et choix d'achat. "
                + "Tu restes bref, clair et courtois. "
                + "Si une question concerne l'administration, le code, le backend ou un sujet hors boutique, tu expliques que tu ne traites que la boutique.";
    }

    private String buildRequestBody(List<ChatMessage> messages) {
        StringBuilder json = new StringBuilder();
        json.append('{');
        json.append("\"model\":\"").append(escapeJson(config.model())).append("\",");
        json.append("\"messages\":[");
        for (int i = 0; i < messages.size(); i++) {
            ChatMessage message = messages.get(i);
            if (i > 0) {
                json.append(',');
            }
            json.append('{')
                    .append("\"role\":\"").append(escapeJson(normalizeRole(message.role()))).append("\",")
                    .append("\"content\":\"").append(escapeJson(safe(message.content()))).append("\"}");
        }
        json.append(']');
        json.append(",\"temperature\":0.4");
        json.append(",\"max_tokens\":700");
        json.append('}');
        return json.toString();
    }

    private static String extractAssistantContent(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        Matcher matcher = CONTENT_PATTERN.matcher(body);
        if (!matcher.find()) {
            return null;
        }
        return unescapeJson(matcher.group(1));
    }


    private static String normalizeRole(String role) {
        String value = safe(role).toLowerCase(Locale.ROOT);
        return switch (value) {
            case "assistant", "system", "user", "tool" -> value;
            default -> "user";
        };
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String trimForError(String value) {
        String text = safe(value);
        if (text.length() <= 500) {
            return text;
        }
        return text.substring(0, 500) + "...";
    }

    private static String escapeJson(String value) {
        String text = value == null ? "" : value;
        StringBuilder out = new StringBuilder(text.length() + 16);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }

    private static String unescapeJson(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c != '\\' || i == value.length() - 1) {
                out.append(c);
                continue;
            }
            char next = value.charAt(++i);
            switch (next) {
                case '"' -> out.append('"');
                case '\\' -> out.append('\\');
                case '/' -> out.append('/');
                case 'b' -> out.append('\b');
                case 'f' -> out.append('\f');
                case 'n' -> out.append('\n');
                case 'r' -> out.append('\r');
                case 't' -> out.append('\t');
                case 'u' -> {
                    if (i + 4 < value.length()) {
                        String hex = value.substring(i + 1, i + 5);
                        try {
                            out.append((char) Integer.parseInt(hex, 16));
                            i += 4;
                        } catch (NumberFormatException ex) {
                            out.append('u').append(hex);
                            i += 4;
                        }
                    }
                }
                default -> out.append(next);
            }
        }
        return out.toString();
    }
}

