package com.pulse.desktop.service;

import com.pulse.desktop.config.AppConfig;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

public class SymfonyVerifyEmailSigner {
    public String buildSignedVerificationUrl(int userId, String email) {
        String secret = AppConfig.appSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("APP_SECRET manquant: impossible de signer le lien de verification.");
        }

        long expires = Instant.now().getEpochSecond() + Math.max(60, AppConfig.verifyEmailLifetimeSeconds());
        String token = createVerifyToken(Integer.toString(userId), email, secret);

        String routeUrl = trimTrailingSlash(AppConfig.webBaseUrl()) + "/verify/email";
        TreeMap<String, String> params = new TreeMap<>();
        params.put("id", Integer.toString(userId));
        params.put("token", token);
        params.put("expires", Long.toString(expires));

        String unsignedCanonical = routeUrl + "?" + toQuery(params);
        String signature = base64Hmac(unsignedCanonical, secret);
        params.put("signature", signature);

        return routeUrl + "?" + toQuery(params);
    }

    private static String createVerifyToken(String userId, String email, String signingKey) {
        String payload = "[\"" + escapeJson(userId) + "\",\"" + escapeJson(email) + "\"]";
        return base64Hmac(payload, signingKey);
    }

    private static String base64Hmac(String value, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(raw);
        } catch (Exception ex) {
            throw new IllegalStateException("Impossible de calculer la signature HMAC.", ex);
        }
    }

    private static String toQuery(Map<String, String> params) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) {
                builder.append('&');
            }
            first = false;
            builder.append(urlEncode(entry.getKey()))
                    .append('=')
                    .append(urlEncode(entry.getValue()));
        }
        return builder.toString();
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://127.0.0.1:8000";
        }
        String out = value.trim();
        while (out.endsWith("/")) {
            out = out.substring(0, out.length() - 1);
        }
        return out;
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
