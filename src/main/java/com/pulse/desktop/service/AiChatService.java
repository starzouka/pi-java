package com.pulse.desktop.service;

import com.pulse.desktop.config.AppConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public class AiChatService {
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public boolean isConfigured() {
        String endpoint = resolveEndpoint();
        return endpoint != null && !endpoint.isBlank();
    }

    public String sendMessage(String message, Map<String, String> context) throws IOException, InterruptedException {
        String endpoint = resolveEndpoint();
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalStateException("PULSE_AI_ENDPOINT non configure.");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("message", message == null ? "" : message);
        payload.put("context", context == null ? Map.of() : context);
        String body = toJson(payload);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint.trim()))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));

        String apiKey = AppConfig.aiApiKey();
        if (apiKey != null && !apiKey.isBlank()) {
            requestBuilder.header("Authorization", "Bearer " + apiKey.trim());
        }

        HttpResponse<String> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        int status = response.statusCode();
        String responseBody = response.body() == null ? "" : response.body();
        if (status < 200 || status >= 300) {
            return "HTTP " + status + ":\n" + responseBody;
        }

        String extracted = extractFirstStringField(responseBody, "reply");
        if (extracted == null) {
            extracted = extractFirstStringField(responseBody, "response");
        }
        if (extracted == null) {
            extracted = extractFirstStringField(responseBody, "message");
        }
        return extracted != null ? extracted : responseBody;
    }

    private static String resolveEndpoint() {
        String configured = AppConfig.aiEndpoint();
        if (configured != null && !configured.isBlank()) {
            return configured.trim();
        }
        if (LocalQrWebServer.isRunning()) {
            String local = LocalQrWebServer.baseUrl();
            if (local != null && !local.isBlank()) {
                return local + "/api/ai/chat";
            }
        }
        return "";
    }

    private static String toJson(Map<String, Object> payload) {
        StringBuilder out = new StringBuilder();
        out.append('{');
        boolean first = true;
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            if (!first) {
                out.append(',');
            }
            first = false;
            out.append('"').append(escapeJson(entry.getKey())).append('"').append(':');
            out.append(toJsonValue(entry.getValue()));
        }
        out.append('}');
        return out.toString();
    }

    private static String toJsonValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String s) {
            return "\"" + escapeJson(s) + "\"";
        }
        if (value instanceof Map<?, ?> map) {
            StringBuilder out = new StringBuilder();
            out.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = entry.getKey() == null ? "" : entry.getKey().toString();
                String v = entry.getValue() == null ? "" : entry.getValue().toString();
                if (!first) {
                    out.append(',');
                }
                first = false;
                out.append('"').append(escapeJson(key)).append('"').append(':');
                out.append('"').append(escapeJson(v)).append('"');
            }
            out.append('}');
            return out.toString();
        }
        return "\"" + escapeJson(value.toString()) + "\"";
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> out.append(c);
            }
        }
        return out.toString();
    }

    private static String extractFirstStringField(String body, String key) {
        if (body == null || body.isBlank() || key == null || key.isBlank()) {
            return null;
        }
        String k = "\"" + key + "\"";
        int idx = body.indexOf(k);
        if (idx < 0) {
            return null;
        }
        int colon = body.indexOf(':', idx + k.length());
        if (colon < 0) {
            return null;
        }
        int firstQuote = body.indexOf('"', colon + 1);
        if (firstQuote < 0) {
            return null;
        }
        int i = firstQuote + 1;
        StringBuilder out = new StringBuilder();
        boolean escaping = false;
        while (i < body.length()) {
            char c = body.charAt(i);
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
                i++;
                continue;
            }
            if (c == '\\') {
                escaping = true;
                i++;
                continue;
            }
            if (c == '"') {
                return out.toString().trim();
            }
            out.append(c);
            i++;
        }
        return null;
    }
}
