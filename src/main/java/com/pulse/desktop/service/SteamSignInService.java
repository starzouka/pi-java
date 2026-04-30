package com.pulse.desktop.service;

import com.pulse.desktop.config.AppConfig;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SteamSignInService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String STEAM_OPENID_ENDPOINT = "https://steamcommunity.com/openid/login";
    private static final Pattern STEAM_ID_PATTERN =
            Pattern.compile("https?://steamcommunity\\.com/openid/id/(\\d{5,25})", Pattern.CASE_INSENSITIVE);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    public record SteamIdentity(String steamId, String displayName) {
    }

    public boolean isFeatureEnabled() {
        return AppConfig.steamSignInEnabled();
    }

    public boolean isEnabled() {
        return isFeatureEnabled();
    }

    public String configurationErrorMessage() {
        if (!isFeatureEnabled()) {
            return "Steam Sign-In est desactive (auth.steam.enabled=false).";
        }
        return "";
    }

    public String configuredRedirectUri() {
        return "http://" + AppConfig.steamRedirectHost() + ":" + AppConfig.steamRedirectPort() + AppConfig.steamRedirectPath();
    }

    public SteamIdentity authenticate() throws IOException, InterruptedException {
        if (!isEnabled()) {
            String reason = configurationErrorMessage();
            if (reason == null || reason.isBlank()) {
                reason = "Steam Sign-In non configure.";
            }
            throw new IOException(reason);
        }

        String state = randomToken(24);
        String redirectUri = configuredRedirectUri();
        String returnTo = appendStateToReturnTo(redirectUri, state);

        CallbackReceiver callbackReceiver = new CallbackReceiver(
                AppConfig.steamRedirectHost(),
                AppConfig.steamRedirectPort(),
                AppConfig.steamRedirectPath()
        );

        try {
            callbackReceiver.start();
            String authUrl = buildAuthorizationUrl(returnTo, steamRealm(redirectUri));
            BrowserService.openUrl(authUrl);

            CallbackResult callback = callbackReceiver.await(AppConfig.steamSignInTimeoutSeconds());
            if (!blank(callback.error())) {
                throw new IOException("Steam a refuse la connexion: " + callback.error());
            }

            String callbackState = callback.params().get("state");
            if (!state.equals(callbackState)) {
                throw new IOException("Etat OAuth/OpenID invalide. Reessayez.");
            }

            if (!verifyOpenIdAssertion(callback.params())) {
                throw new IOException("Validation OpenID Steam echouee.");
            }

            String claimedId = pickClaimedId(callback.params());
            String steamId = extractSteamId(claimedId);
            if (blank(steamId)) {
                throw new IOException("Steam ID introuvable dans la reponse OpenID.");
            }

            String displayName = resolveDisplayName(steamId);
            if (blank(displayName)) {
                displayName = "Steam Player " + lastDigits(steamId, 4);
            }
            return new SteamIdentity(steamId, displayName);
        } finally {
            callbackReceiver.close();
        }
    }

    private String buildAuthorizationUrl(String returnTo, String realm) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("openid.ns", "http://specs.openid.net/auth/2.0");
        params.put("openid.mode", "checkid_setup");
        params.put("openid.return_to", returnTo);
        params.put("openid.realm", realm);
        params.put("openid.identity", "http://specs.openid.net/auth/2.0/identifier_select");
        params.put("openid.claimed_id", "http://specs.openid.net/auth/2.0/identifier_select");

        StringBuilder url = new StringBuilder(STEAM_OPENID_ENDPOINT).append('?');
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) {
                url.append('&');
            }
            url.append(encode(entry.getKey())).append('=').append(encode(entry.getValue()));
            first = false;
        }
        return url.toString();
    }

    private boolean verifyOpenIdAssertion(Map<String, String> params) throws IOException, InterruptedException {
        if (params == null || params.isEmpty()) {
            return false;
        }

        Map<String, String> verification = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            if (key == null || !key.startsWith("openid.")) {
                continue;
            }
            verification.put(key, entry.getValue());
        }
        if (verification.isEmpty()) {
            return false;
        }
        verification.put("openid.mode", "check_authentication");

        HttpRequest request = HttpRequest.newBuilder(URI.create(STEAM_OPENID_ENDPOINT))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form(verification), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            return false;
        }
        String body = response.body() == null ? "" : response.body().toLowerCase(Locale.ROOT);
        return body.contains("is_valid:true");
    }

    private String resolveDisplayName(String steamId) {
        String byWebApi = resolveDisplayNameBySteamWebApi(steamId);
        if (!blank(byWebApi)) {
            return byWebApi;
        }
        return resolveDisplayNameByPublicProfileXml(steamId);
    }

    private String resolveDisplayNameBySteamWebApi(String steamId) {
        String apiKey = AppConfig.steamApiKey();
        if (blank(apiKey) || blank(steamId)) {
            return "";
        }
        String url = "https://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/?key="
                + encode(apiKey)
                + "&steamids="
                + encode(steamId);
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(12))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return "";
            }
            String body = response.body() == null ? "" : response.body();
            return extractJsonString(body, "personaname");
        } catch (Exception ex) {
            return "";
        }
    }

    private String resolveDisplayNameByPublicProfileXml(String steamId) {
        if (blank(steamId)) {
            return "";
        }
        String url = "https://steamcommunity.com/profiles/" + encode(steamId) + "/?xml=1";
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(12))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return "";
            }
            String body = response.body() == null ? "" : response.body();
            String withCdata = extractBetween(body, "<steamID><![CDATA[", "]]></steamID>");
            if (!blank(withCdata)) {
                return withCdata;
            }
            return extractBetween(body, "<steamID>", "</steamID>");
        } catch (Exception ex) {
            return "";
        }
    }

    private static String extractBetween(String source, String start, String end) {
        if (source == null || start == null || end == null) {
            return "";
        }
        int i = source.indexOf(start);
        if (i < 0) {
            return "";
        }
        int from = i + start.length();
        int j = source.indexOf(end, from);
        if (j < 0) {
            return "";
        }
        return source.substring(from, j).trim();
    }

    private static String extractJsonString(String json, String field) {
        if (blank(json) || blank(field)) {
            return "";
        }
        String regex = "\"" + Pattern.quote(field) + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"";
        Matcher matcher = Pattern.compile(regex, Pattern.DOTALL).matcher(json);
        if (!matcher.find()) {
            return "";
        }
        return unescapeJsonString(matcher.group(1));
    }

    private static String pickClaimedId(Map<String, String> params) {
        String claimed = params == null ? null : params.get("openid.claimed_id");
        if (!blank(claimed)) {
            return claimed;
        }
        String identity = params == null ? null : params.get("openid.identity");
        return identity == null ? "" : identity.trim();
    }

    private static String extractSteamId(String claimedId) {
        if (blank(claimedId)) {
            return "";
        }
        Matcher matcher = STEAM_ID_PATTERN.matcher(claimedId.trim());
        if (!matcher.find()) {
            return "";
        }
        return matcher.group(1);
    }

    private static String appendStateToReturnTo(String redirectUri, String state) {
        if (redirectUri.contains("?")) {
            return redirectUri + "&state=" + encode(state);
        }
        return redirectUri + "?state=" + encode(state);
    }

    private static String steamRealm(String redirectUri) throws IOException {
        try {
            URI uri = URI.create(redirectUri);
            String host = uri.getHost();
            int port = uri.getPort();
            if (blank(host)) {
                throw new IOException("Host de redirection Steam invalide.");
            }
            StringBuilder realm = new StringBuilder(uri.getScheme() == null ? "http" : uri.getScheme());
            realm.append("://").append(host);
            if (port > 0) {
                realm.append(":").append(port);
            }
            return realm.toString();
        } catch (IllegalArgumentException ex) {
            throw new IOException("Redirect URI Steam invalide.", ex);
        }
    }

    private static String form(Map<String, String> params) {
        StringBuilder body = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) {
                body.append('&');
            }
            body.append(encode(entry.getKey())).append('=').append(encode(entry.getValue()));
            first = false;
        }
        return body.toString();
    }

    private static String randomToken(int minLength) {
        int length = Math.max(24, minLength);
        byte[] bytes = new byte[length];
        SECURE_RANDOM.nextBytes(bytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static String decode(String value) {
        return URLDecoder.decode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static String unescapeJsonString(String value) {
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c != '\\' || i + 1 >= value.length()) {
                sb.append(c);
                continue;
            }
            char next = value.charAt(++i);
            switch (next) {
                case '"' -> sb.append('"');
                case '\\' -> sb.append('\\');
                case '/' -> sb.append('/');
                case 'b' -> sb.append('\b');
                case 'f' -> sb.append('\f');
                case 'n' -> sb.append('\n');
                case 'r' -> sb.append('\r');
                case 't' -> sb.append('\t');
                case 'u' -> {
                    if (i + 4 >= value.length()) {
                        sb.append("\\u");
                        continue;
                    }
                    String hex = value.substring(i + 1, i + 5);
                    try {
                        sb.append((char) Integer.parseInt(hex, 16));
                    } catch (NumberFormatException ex) {
                        sb.append("\\u").append(hex);
                    }
                    i += 4;
                }
                default -> sb.append(next);
            }
        }
        return sb.toString();
    }

    private static String lastDigits(String value, int size) {
        if (blank(value)) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= size) {
            return trimmed;
        }
        return trimmed.substring(trimmed.length() - size);
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private record CallbackResult(Map<String, String> params, String error) {
    }

    private static final class CallbackReceiver implements AutoCloseable {
        private final String host;
        private final int port;
        private final String callbackPath;
        private final CompletableFuture<CallbackResult> callbackFuture = new CompletableFuture<>();
        private HttpServer server;

        private CallbackReceiver(String host, int port, String callbackPath) {
            this.host = host;
            this.port = port;
            this.callbackPath = callbackPath;
        }

        private void start() throws IOException {
            server = HttpServer.create(new InetSocketAddress(host, port), 0);
            server.createContext(callbackPath, this::handleCallback);
            server.start();
        }

        private CallbackResult await(int timeoutSeconds) throws IOException, InterruptedException {
            try {
                return callbackFuture.get(Math.max(30, timeoutSeconds), TimeUnit.SECONDS);
            } catch (java.util.concurrent.TimeoutException ex) {
                throw new IOException("Delai depasse pendant la connexion Steam.");
            } catch (java.util.concurrent.ExecutionException ex) {
                Throwable cause = ex.getCause();
                if (cause instanceof IOException ioEx) {
                    throw ioEx;
                }
                throw new IOException("Echec callback Steam.", cause);
            }
        }

        private void handleCallback(HttpExchange exchange) throws IOException {
            Map<String, String> query = parseQuery(exchange.getRequestURI());
            String error = query.get("openid.mode");
            if ("cancel".equalsIgnoreCase(error)) {
                error = "cancel";
            } else {
                error = "";
            }

            String html = """
                    <!doctype html>
                    <html lang="en">
                    <head><meta charset="utf-8"><title>PULSE Steam Sign-In</title></head>
                    <body style="font-family:Segoe UI,Arial,sans-serif;background:#10141c;color:#f3f5fa;padding:28px;">
                      <h2 style="margin:0 0 10px;">Steam Sign-In</h2>
                      <p style="margin:0;">You can close this tab and return to PULSE desktop.</p>
                    </body>
                    </html>
                    """;
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
            callbackFuture.complete(new CallbackResult(query, error));
        }

        private static Map<String, String> parseQuery(URI uri) {
            Map<String, String> values = new LinkedHashMap<>();
            if (uri == null || uri.getRawQuery() == null || uri.getRawQuery().isBlank()) {
                return values;
            }
            String[] chunks = uri.getRawQuery().split("&");
            for (String chunk : chunks) {
                if (chunk == null || chunk.isBlank()) {
                    continue;
                }
                int sep = chunk.indexOf('=');
                if (sep < 0) {
                    values.put(decode(chunk), "");
                    continue;
                }
                String key = decode(chunk.substring(0, sep));
                String value = decode(chunk.substring(sep + 1));
                values.put(key, value);
            }
            return values;
        }

        @Override
        public void close() {
            if (server != null) {
                server.stop(0);
            }
        }
    }
}
