package com.pulse.desktop.service;

import com.pulse.desktop.config.AppConfig;
import com.pulse.desktop.model.CategoryModel;
import com.pulse.desktop.model.GameModel;
import com.pulse.desktop.repo.CategoryRepository;
import com.pulse.desktop.repo.GameRepository;
import com.pulse.desktop.service.ActivityLogService;
import com.pulse.desktop.util.ImageResolver;
import com.pulse.desktop.util.UrlUtils;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class LocalQrWebServer {
    private static final Object LOCK = new Object();
    private static HttpServer server;
    private static ExecutorService executor;
    private static int boundPort;

    private LocalQrWebServer() {
    }

    public static boolean isRunning() {
        synchronized (LOCK) {
            return server != null;
        }
    }

    public static int port() {
        synchronized (LOCK) {
            return boundPort;
        }
    }

    public static String baseUrl() {
        synchronized (LOCK) {
            if (server == null || boundPort <= 0) {
                return null;
            }
            return "http://127.0.0.1:" + boundPort;
        }
    }

    public static String deviceBaseUrl() {
        String base = baseUrl();
        if (base == null) {
            return null;
        }
        return UrlUtils.toDeviceAccessibleBaseUrl(base);
    }

    public static void startIfEnabled() {
        if (!AppConfig.qrServerEnabled()) {
            return;
        }
        start();
    }

    public static void start() {
        synchronized (LOCK) {
            if (server != null) {
                return;
            }

            int desiredPort = Math.max(1, AppConfig.qrServerPort());
            try {
                server = HttpServer.create(new InetSocketAddress("0.0.0.0", desiredPort), 0);
                boundPort = desiredPort;
            } catch (IOException ex) {
                try {
                    server = HttpServer.create(new InetSocketAddress("0.0.0.0", 0), 0);
                    boundPort = server.getAddress().getPort();
                } catch (IOException inner) {
                    server = null;
                    boundPort = 0;
                    return;
                }
            }

            executor = Executors.newFixedThreadPool(4);
            server.setExecutor(executor);

            server.createContext("/", new IndexHandler());
            server.createContext("/media", new MediaHandler());
            server.createContext("/api/ai/chat", new AiMockHandler());
            server.createContext("/games/", new GameHandler());
            server.createContext("/categories/", new CategoryHandler());

            server.start();
        }
    }

    public static void stop() {
        synchronized (LOCK) {
            if (server != null) {
                server.stop(0);
                server = null;
                boundPort = 0;
            }
            if (executor != null) {
                executor.shutdownNow();
                executor = null;
            }
        }
    }

    private static void sendHtml(HttpExchange exchange, int status, String html) throws IOException {
        byte[] bytes = (html == null ? "" : html).getBytes(StandardCharsets.UTF_8);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "text/html; charset=UTF-8");
        headers.set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static void sendBytes(HttpExchange exchange, int status, String contentType, byte[] bytes) throws IOException {
        byte[] safe = bytes == null ? new byte[0] : bytes;
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", contentType == null ? "application/octet-stream" : contentType);
        headers.set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, safe.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(safe);
        }
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        sendBytes(exchange, status, "application/json; charset=UTF-8",
                (json == null ? "" : json).getBytes(StandardCharsets.UTF_8));
    }

    private static void sendNotFound(HttpExchange exchange, String message) throws IOException {
        sendHtml(exchange, 404, page("Introuvable", "<p>" + escapeHtml(message) + "</p>"));
    }

    private static String page(String title, String bodyHtml) {
        String template = """
                <!doctype html>
                <html lang="fr">
                <head>
                  <meta charset="utf-8"/>
                  <meta name="viewport" content="width=device-width, initial-scale=1"/>
                  <title>{{title}}</title>
                  <style>
                    :root { color-scheme: dark; }
                    body { margin:0; font-family: system-ui, -apple-system, Segoe UI, Roboto, Arial, sans-serif; background:#0b1324; color:#e8eefc; }
                    .wrap { max-width: 860px; margin: 0 auto; padding: 18px; }
                    .card { background: rgba(255,255,255,0.06); border: 1px solid rgba(255,255,255,0.10); border-radius: 18px; padding: 16px; }
                    h1 { margin: 0 0 8px 0; font-size: 22px; }
                    .muted { color: rgba(232,238,252,0.70); }
                    .chips { display:flex; gap:8px; flex-wrap:wrap; margin-top:10px; }
                    .chip { font-size: 12px; padding: 4px 10px; border-radius: 999px; background: rgba(255,255,255,0.08); border: 1px solid rgba(255,255,255,0.10); }
                    a { color: #8ab4ff; }
                    .actions { display:flex; gap:10px; flex-wrap:wrap; margin-top:14px; }
                    .btn { appearance:none; border:1px solid rgba(255,255,255,0.12); background: rgba(255,255,255,0.08); color:#e8eefc; padding: 9px 12px; border-radius: 12px; font-size: 14px; text-decoration:none; }
                    .btn:active { transform: translateY(1px); }
                    .btn--primary { background: rgba(138,180,255,0.20); border-color: rgba(138,180,255,0.35); }
                    .media { width:100%; aspect-ratio: 16/9; border-radius: 14px; overflow:hidden; border:1px solid rgba(255,255,255,0.10); background: rgba(255,255,255,0.04); margin-bottom: 12px; }
                    .media img { width:100%; height:100%; object-fit: cover; display:block; }
                    .list { margin-top: 14px; display:flex; flex-direction:column; gap:10px; }
                    .item { padding: 12px; border-radius: 14px; border:1px solid rgba(255,255,255,0.10); background: rgba(0,0,0,0.18); }
                    .item-title { margin:0 0 6px 0; font-size:16px; }
                    .item-meta { font-size:12px; color: rgba(232,238,252,0.70); }
                    pre { white-space: pre-wrap; word-break: break-word; background: rgba(0,0,0,0.25); padding: 10px; border-radius: 12px; }
                  </style>
                  <script>
                    async function copyText(text) {
                      try {
                        if (navigator.clipboard && window.isSecureContext) {
                          await navigator.clipboard.writeText(text);
                          toast('Copié');
                          return;
                        }
                      } catch (e) {}
                      const ok = prompt('Copier:', text);
                      if (ok !== null) toast('Copiez puis OK');
                    }
                    let toastTimer;
                    function toast(msg) {
                      let el = document.getElementById('toast');
                      if (!el) {
                        el = document.createElement('div');
                        el.id = 'toast';
                        el.style.position = 'fixed';
                        el.style.left = '50%';
                        el.style.bottom = '18px';
                        el.style.transform = 'translateX(-50%)';
                        el.style.background = 'rgba(0,0,0,0.70)';
                        el.style.border = '1px solid rgba(255,255,255,0.14)';
                        el.style.color = '#e8eefc';
                        el.style.padding = '10px 12px';
                        el.style.borderRadius = '12px';
                        el.style.zIndex = '9999';
                        document.body.appendChild(el);
                      }
                      el.textContent = msg;
                      el.style.display = 'block';
                      if (toastTimer) clearTimeout(toastTimer);
                      toastTimer = setTimeout(() => { el.style.display = 'none'; }, 1600);
                    }
                  </script>
                </head>
                <body>
                  <div class="wrap">
                    <div class="card">
                      {{body}}
                    </div>
                  </div>
                </body>
                </html>
                """;
        return template
                .replace("{{title}}", escapeHtml(title))
                .replace("{{body}}", bodyHtml == null ? "" : bodyHtml);
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
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

    private static String readBody(HttpExchange exchange) throws IOException {
        if (exchange == null) {
            return "";
        }
        try (InputStream in = exchange.getRequestBody()) {
            if (in == null) {
                return "";
            }
            byte[] bytes = in.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        }
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

    private static String tail(HttpExchange exchange, String prefix) {
        String path = exchange.getRequestURI() == null ? "" : exchange.getRequestURI().getPath();
        if (path == null) {
            return "";
        }
        if (!path.startsWith(prefix)) {
            return "";
        }
        String raw = path.substring(prefix.length());
        raw = raw.startsWith("/") ? raw.substring(1) : raw;
        return URLDecoder.decode(raw, StandardCharsets.UTF_8);
    }

    private static Map<String, String> queryParams(HttpExchange exchange) {
        Map<String, String> out = new HashMap<>();
        URI uri = exchange.getRequestURI();
        String raw = uri == null ? null : uri.getRawQuery();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        String[] pairs = raw.split("&");
        for (String pair : pairs) {
            if (pair.isBlank()) {
                continue;
            }
            String[] chunks = pair.split("=", 2);
            String key = URLDecoder.decode(chunks[0], StandardCharsets.UTF_8).trim();
            String value = chunks.length > 1 ? URLDecoder.decode(chunks[1], StandardCharsets.UTF_8).trim() : "";
            if (!key.isBlank()) {
                out.put(key, value);
            }
        }
        return out;
    }

    private static Path resolveSafeFile(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return null;
        }
        String normalized = rawPath.replace('\\', '/').trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.contains("..")) {
            return null;
        }

        Path webRoot = AppConfig.webRootPath();
        Path[] roots = new Path[]{
                webRoot.toAbsolutePath().normalize(),
                webRoot.resolve("public").toAbsolutePath().normalize(),
                Paths.get("").toAbsolutePath().normalize()
        };

        for (Path root : roots) {
            Path candidate = root.resolve(normalized).normalize();
            if (!candidate.startsWith(root)) {
                continue;
            }
            if (Files.exists(candidate) && Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static String contentType(Path path) {
        if (path == null) {
            return "application/octet-stream";
        }
        String name = path.getFileName() == null ? "" : path.getFileName().toString().toLowerCase();
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".webp")) return "image/webp";
        if (name.endsWith(".gif")) return "image/gif";
        if (name.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }

    private static Integer parseIntOrNull(String value) {
        if (value == null) {
            return null;
        }
        String t = value.trim();
        if (t.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(t);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static final class IndexHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if (exchange.getRequestURI() != null && "/".equals(exchange.getRequestURI().getPath())) {
                    String body = """
                            <h1>PULSE QR Server</h1>
                            <p class="muted">Serveur embarque de l'application JavaFX (pour ouvrir les QR codes sur telephone).</p>
                            <div class="chips">
                              <span class="chip">DB: %s</span>
                              <span class="chip">Port: %d</span>
                            </div>
                            <p class="muted" style="margin-top:12px">Exemples:</p>
                            <pre>/games/1\n/categories/1</pre>
                            """.formatted(escapeHtml(AppConfig.dbName()), port());
                    sendHtml(exchange, 200, page("PULSE QR Server", body));
                    return;
                }
                sendNotFound(exchange, "Page inconnue.");
            } catch (Exception ex) {
                sendHtml(exchange, 500, page("Erreur", "<p>Erreur serveur.</p><pre>" + escapeHtml(ex.getMessage()) + "</pre>"));
            }
        }
    }

    private static final class MediaHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                Map<String, String> params = queryParams(exchange);
                String rawPath = params.get("path");
                Path file = resolveSafeFile(rawPath);
                if (file == null) {
                    sendNotFound(exchange, "Media introuvable.");
                    return;
                }
                byte[] bytes = Files.readAllBytes(file);
                sendBytes(exchange, 200, contentType(file), bytes);
            } catch (Exception ex) {
                sendHtml(exchange, 500, page("Erreur", "<p>Erreur media.</p><pre>" + escapeHtml(ex.getMessage()) + "</pre>"));
            }
        }
    }

    private static final class AiMockHandler implements HttpHandler {
        private final GameRepository gameRepo = new GameRepository();
        private final CategoryRepository categoryRepo = new CategoryRepository();

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String method = exchange.getRequestMethod() == null ? "" : exchange.getRequestMethod().toUpperCase();
                if (!"POST".equals(method)) {
                    sendJson(exchange, 405, "{\"reply\":\"Method not allowed (POST only)\",\"mode\":\"mock\"}");
                    return;
                }

                String body = readBody(exchange);
                String message = extractFirstStringField(body, "message");
                if (message == null) {
                    message = "";
                }

                String page = firstNonBlank(extractFirstStringField(body, "page"), "unknown");

                String formName = firstNonBlank(extractFirstStringField(body, "form_name"), "");
                String formSlug = firstNonBlank(extractFirstStringField(body, "form_slug"), "");
                String formStatus = firstNonBlank(extractFirstStringField(body, "form_status"), "");
                String formPublisher = firstNonBlank(extractFirstStringField(body, "form_publisher"), "");
                String formCategory = firstNonBlank(extractFirstStringField(body, "form_category"), "");
                String formDescription = firstNonBlank(extractFirstStringField(body, "form_description"), "");
                String formCover = firstNonBlank(extractFirstStringField(body, "form_cover"), "");

                String msgOneLine = message.replace("\n", " ").trim();
                StringBuilder reply = new StringBuilder();
                String msgLower = msgOneLine.toLowerCase();

                reply.append("(Mode test - mock)\n");

                // Always echo a short form snapshot (helps user understand context passed to API).
                if ("admin_games".equals(page)) {
                    reply.append("Formulaire (Jeu): ")
                            .append("nom=").append(valueOrDash(formName))
                            .append(", categorie=").append(valueOrDash(formCategory))
                            .append(", slug=").append(valueOrDash(formSlug))
                            .append(", statut=").append(valueOrDash(formStatus))
                            .append(", editeur=").append(valueOrDash(formPublisher))
                            .append(", cover=").append(formCover.isBlank() ? "-" : "OK")
                            .append("\n\n");
                } else if ("admin_categories".equals(page)) {
                    reply.append("Formulaire (Categorie): ")
                            .append("nom=").append(valueOrDash(formName))
                            .append(", slug=").append(valueOrDash(formSlug))
                            .append(", description=").append(formDescription.isBlank() ? "-" : "OK")
                            .append("\n\n");
                }

                if (wantsAutofill(msgLower)) {
                    reply.append(buildAutofill(page, formName, formCategory, formSlug, formStatus, formPublisher, formDescription));
                } else if (wantsChecklist(msgLower)) {
                    reply.append(buildChecklist(page, formName, formCategory, formSlug, formStatus, formPublisher, formCover, formDescription));
                } else if (wantsCoverHelp(msgLower)) {
                    reply.append(coverHelp(page));
                } else if (wantsDescription(msgLower)) {
                    reply.append(buildDescription(page, formName, formCategory));
                } else if (wantsTags(msgLower)) {
                    String base = !formName.isBlank() ? formName : extractQuoted(msgOneLine);
                    reply.append(buildTags(page, base, formCategory));
                } else if (wantsCategorySuggestion(msgLower)) {
                    String base = !formName.isBlank() ? formName : extractQuoted(msgOneLine);
                    reply.append(suggestCategory(base));
                } else if (wantsMarketing(msgLower)) {
                    String base = !formName.isBlank() ? formName : extractQuoted(msgOneLine);
                    reply.append(buildMarketing(base, formCategory));
                } else if (wantsDifference(msgLower)) {
                    reply.append(buildDifference(msgOneLine));
                } else if (wantsSlugValidation(msgLower)) {
                    reply.append(validateSlug(page, msgOneLine, formSlug, formName));
                } else if (wantsStatus(msgLower)) {
                    reply.append(statusSuggestion(msgLower));
                } else if (wantsSlug(msgLower)) {
                    String base = !formName.isBlank() ? formName : extractQuoted(msgOneLine);
                    if (base.isBlank()) {
                        reply.append("Donne-moi le nom du jeu/categorie (ou remplis le champ Nom), et je propose un slug.\n");
                    } else {
                        reply.append(slugSuggestions(page, base));
                    }
                } else {
                    // Default: short helpful response, not the same block for every message.
                    reply.append("Je peux aider sur: slug, statut, cover, description.\n");
                    reply.append("Exemples:\n");
                    reply.append("- \"Analyse mon formulaire avant Ajouter\"\n");
                    reply.append("- \"Propose un slug pour \\\"Call of Duty Warzone\\\"\"\n");
                    reply.append("- \"Quel statut choisir pour publier ?\"\n");
                    reply.append("- \"Comment importer la cover ?\"\n");
                }

                String json = "{\"reply\":\"" + escapeJson(reply.toString()) + "\",\"mode\":\"mock\"}";
                sendJson(exchange, 200, json);
            } catch (Exception ex) {
                sendJson(exchange, 500, "{\"reply\":\"Erreur mock\",\"mode\":\"mock\"}");
            }
        }

        private String slugSuggestions(String page, String baseName) {
            String base = slugify(baseName);
            String s1 = ensureUniqueSlug(page, base);
            String s2 = ensureUniqueSlug(page, base + "-2026");
            String s3 = ensureUniqueSlug(page, base + "-game");
            return "Slugs proposes:\n- " + s1 + "\n- " + s2 + "\n- " + s3 + "\n";
        }

        private String ensureUniqueSlug(String page, String slug) {
            if (slug == null || slug.isBlank()) {
                return slug;
            }
            try {
                if ("admin_categories".equals(page)) {
                    if (categoryRepo.findBySlug(slug) == null) {
                        return slug;
                    }
                } else {
                    if (gameRepo.findBySlug(slug) == null) {
                        return slug;
                    }
                }
            } catch (Exception ignored) {
                return slug;
            }
            return slug + "-" + (System.currentTimeMillis() % 1000);
        }

        private String validateSlug(String page, String msgOneLine, String formSlug, String formName) {
            String candidate = firstNonBlank(formSlug, "");
            if (candidate.isBlank()) {
                // Try extracting a quoted slug, otherwise fallback to slugify(name).
                candidate = extractQuoted(msgOneLine);
            }
            if (candidate.isBlank()) {
                candidate = slugify(firstNonBlank(formName, ""));
            }
            if (candidate.isBlank()) {
                return "Donne-moi un slug (champ Slug) ou un nom (champ Nom) pour que je puisse valider.\n";
            }

            String normalized = slugify(candidate);
            boolean changed = !normalized.equals(candidate);
            String unique = ensureUniqueSlug(page, normalized);
            boolean uniqueAdjusted = !unique.equals(normalized);

            StringBuilder out = new StringBuilder();
            out.append("Validation slug:\n");
            out.append("- Saisi: ").append(candidate).append("\n");
            out.append("- Normalise: ").append(normalized).append(changed ? " (corrige)\n" : " (ok)\n");
            if (uniqueAdjusted) {
                out.append("- Unique: ").append(unique).append(" (slug deja pris)\n");
            } else {
                out.append("- Unique: ").append(unique).append(" (disponible)\n");
            }
            out.append("\nConseil: utilise minuscules + tirets, pas d'espaces.\n");
            return out.toString();
        }

        private String buildAutofill(
                String page,
                String formName,
                String formCategory,
                String formSlug,
                String formStatus,
                String formPublisher,
                String formDescription
        ) {
            if (!"admin_games".equals(page)) {
                return "Auto-remplissage disponible sur Administration -> Jeux.\n";
            }
            String baseName = firstNonBlank(formName, "");

            String slug = firstNonBlank(formSlug, "");
            if (slug.isBlank()) {
                slug = slugify(baseName);
            }
            slug = ensureUniqueSlug(page, slug);

            String status = firstNonBlank(formStatus, "");
            if (status.isBlank()) {
                status = "DRAFT";
            } else {
                status = status.trim().toUpperCase();
                if (!status.contains("PUBLISHED")) {
                    status = "DRAFT";
                } else {
                    status = "PUBLISHED";
                }
            }

            String publisher = firstNonBlank(formPublisher, "");
            if (publisher.isBlank()) {
                publisher = guessPublisher(baseName);
            }

            String desc = firstNonBlank(formDescription, "");
            if (desc.isBlank()) {
                String proposed = buildDescription(page, baseName, formCategory).trim();
                if (proposed.toLowerCase().startsWith("description proposee:")) {
                    int colon = proposed.indexOf(':');
                    desc = colon < 0 ? proposed : proposed.substring(colon + 1).trim();
                } else {
                    desc = proposed;
                }
            }

            return "Slug: " + valueOrDash(slug) + "\n"
                    + "Statut: " + valueOrDash(status) + "\n"
                    + "Editeur: " + valueOrDash(publisher) + "\n"
                    + "Description: " + valueOrDash(desc) + "\n";
        }

        private static String guessPublisher(String gameName) {
            String n = gameName == null ? "" : gameName.trim().toLowerCase();
            if (n.contains("valorant") || n.contains("league of legends")) return "Riot Games";
            if (n.contains("call of duty") || n.contains("warzone")) return "Activision";
            if (n.contains("fifa") || n.contains("ea fc")) return "EA Sports";
            if (n.contains("starcraft")) return "Blizzard";
            return "Independant";
        }
    }

    private static boolean wantsChecklist(String msgLower) {
        return msgLower.contains("analyse")
                || msgLower.contains("formulaire")
                || msgLower.contains("manque")
                || msgLower.contains("avant de cliquer")
                || msgLower.contains("checklist");
    }

    private static boolean wantsAutofill(String msgLower) {
        if (msgLower == null || msgLower.isBlank()) {
            return false;
        }
        return msgLower.contains("auto-rempl")
                || msgLower.contains("autorempl")
                || (msgLower.contains("auto") && (msgLower.contains("rempl") || msgLower.contains("champs")));
    }

    private static boolean wantsSlug(String msgLower) {
        return msgLower.contains("slug") || msgLower.contains("url") || msgLower.contains("lien");
    }

    private static boolean wantsStatus(String msgLower) {
        return msgLower.contains("statut") || msgLower.contains("draft") || msgLower.contains("published")
                || msgLower.contains("publier") || msgLower.contains("public");
    }

    private static boolean wantsDescription(String msgLower) {
        return msgLower.contains("description") || msgLower.contains("decris") || msgLower.contains("décris")
                || msgLower.contains("decrire") || msgLower.contains("décrire");
    }

    private static boolean wantsCoverHelp(String msgLower) {
        return msgLower.contains("cover") || msgLower.contains("photo") || msgLower.contains("image");
    }

    private static boolean wantsTags(String msgLower) {
        return msgLower.contains("tag")
                || msgLower.contains("keyword")
                || msgLower.contains("mots cles")
                || msgLower.contains("mots-cles")
                || msgLower.contains("mot cle")
                || msgLower.contains("mot-cle");
    }

    private static boolean wantsCategorySuggestion(String msgLower) {
        return msgLower.contains("propose une categorie")
                || msgLower.contains("categorie adaptee")
                || msgLower.contains("catégorie adaptée")
                || (msgLower.contains("quelle categorie") && msgLower.contains("jeu"))
                || (msgLower.contains("categorie") && msgLower.contains("adapt") && msgLower.contains("jeu"));
    }

    private static boolean wantsMarketing(String msgLower) {
        return (msgLower.contains("marketing") || msgLower.contains("presentation") || msgLower.contains("présentation"))
                && (msgLower.contains("texte") || msgLower.contains("2 lignes") || msgLower.contains("deux lignes"));
    }

    private static boolean wantsDifference(String msgLower) {
        return (msgLower.contains("difference") || msgLower.contains("différence"))
                && msgLower.contains("entre");
    }

    private static boolean wantsSlugValidation(String msgLower) {
        return msgLower.contains("slug")
                && (msgLower.contains("valide")
                || msgLower.contains("valider")
                || msgLower.contains("corrige")
                || msgLower.contains("corriger")
                || msgLower.contains("invalide")
                || msgLower.contains("invalid"));
    }

    private static String buildChecklist(
            String page,
            String formName,
            String formCategory,
            String formSlug,
            String formStatus,
            String formPublisher,
            String formCover,
            String formDescription
    ) {
        StringBuilder out = new StringBuilder();
        out.append("Checklist avant \"Ajouter\":\n");
        if ("admin_games".equals(page)) {
            if (formName.isBlank()) out.append("- Nom du jeu: manquant (obligatoire)\n");
            if (formCategory.isBlank()) out.append("- Categorie: manquante (obligatoire)\n");
            if (formStatus.isBlank()) out.append("- Statut: vide (optionnel, par defaut DRAFT)\n");
            if (formSlug.isBlank() && !formName.isBlank()) {
                out.append("- Slug: vide (optionnel). Suggestion: ").append(slugify(formName)).append("\n");
            }
            if (formPublisher.isBlank()) out.append("- Editeur: vide (optionnel)\n");
            if (formCover.isBlank()) out.append("- Cover: vide (optionnel)\n");
            out.append("\nAction conseillee:\n");
            out.append("- Si tu as une image: clique \"Importer cover\" puis \"Ajouter\" / \"Modifier Selection\"\n");
        } else if ("admin_categories".equals(page)) {
            if (formName.isBlank()) out.append("- Nom de la categorie: manquant (obligatoire)\n");
            if (formSlug.isBlank() && !formName.isBlank()) {
                out.append("- Slug: vide (optionnel). Suggestion: ").append(slugify(formName)).append("\n");
            }
            if (formDescription.isBlank()) out.append("- Description: vide (optionnel)\n");
            out.append("\nAction conseillee:\n");
            out.append("- Option: clique \"IA: Decrire\" pour remplir la description rapidement\n");
        } else {
            out.append("- Je ne vois pas de contexte de formulaire (page inconnue).\n");
        }
        return out.toString();
    }

    private static String coverHelp(String page) {
        if (!"admin_games".equals(page)) {
            return "La cover (photo) s'applique aux jeux. Ouvre Administration -> Jeux.\n";
        }
        return """
                Pour ajouter une cover:
                - Clique "Importer cover" (png/jpg/jpeg/webp/gif)
                - Puis clique "Ajouter" (nouveau jeu) ou "Modifier Selection" (jeu existant)

                Recommandation:
                - Format: JPG/PNG
                - Ratio: 16:9 (ex: 1280x720)
                """;
    }

    private static String buildDescription(String page, String name, String category) {
        if ("admin_categories".equals(page)) {
            String base = name.isBlank() ? "cette categorie" : ("\"" + name + "\"");
            return "Description proposee: " + base + " regroupe des jeux adaptes aux joueurs recherchant des experiences competitives et fun.\n";
        }
        if ("admin_games".equals(page)) {
            String base = name.isBlank() ? "Ce jeu" : ("\"" + name + "\"");
            String cat = category.isBlank() ? "sa categorie" : ("la categorie " + category);
            return base + " est un jeu populaire de " + cat + ", ideal pour des sessions rapides et competitives.\n";
        }
        return "Donne-moi le nom (jeu/categorie) et je propose une description courte.\n";
    }

    private static String buildTags(String page, String nameOrEmpty, String categoryOrEmpty) {
        String name = firstNonBlank(nameOrEmpty, "");
        String category = firstNonBlank(categoryOrEmpty, "");
        String source = !category.isBlank() ? category : guessCategoryForGame(name);

        String[] tags = switch (source.trim().toLowerCase()) {
            case "rts" -> new String[]{"strategie", "temps-reel", "gestion", "multijoueur", "esport"};
            case "fps" -> new String[]{"fps", "tir", "action", "competitive", "multijoueur"};
            case "battle royale" -> new String[]{"battle-royale", "survie", "loot", "multijoueur", "competitive"};
            case "moba" -> new String[]{"moba", "5v5", "strategie", "ranked", "esport"};
            case "sports" -> new String[]{"sports", "competition", "simulation", "multijoueur", "saison"};
            case "sandbox" -> new String[]{"sandbox", "creatif", "construction", "exploration", "survie"};
            default -> new String[]{"jeux-video", "multijoueur", "fun", "challenge", "communautaire"};
        };

        StringBuilder out = new StringBuilder();
        out.append("Tags proposes");
        if (!name.isBlank()) {
            out.append(" pour \"").append(name).append("\"");
        }
        out.append(":\n");
        for (String t : tags) {
            out.append("- ").append(t).append("\n");
        }
        if (!"admin_games".equals(page)) {
            out.append("\nNote: les tags sont surtout utiles sur la fiche Jeu.\n");
        }
        return out.toString();
    }

    private static String suggestCategory(String nameOrEmpty) {
        String name = firstNonBlank(nameOrEmpty, "").trim();
        if (name.isBlank()) {
            return "Donne-moi le nom du jeu (ex: \"League of Legends\") et je propose une categorie.\n";
        }
        String guess = guessCategoryForGame(name);
        if (guess.isBlank()) {
            return "Je n'arrive pas a deviner la categorie. Indique 1 mot sur le gameplay (ex: tir / strategie / sport).\n";
        }
        return "Categorie proposee pour \"" + name + "\": " + guess + "\n";
    }

    private static String buildMarketing(String nameOrEmpty, String categoryOrEmpty) {
        String name = firstNonBlank(nameOrEmpty, "").trim();
        String category = firstNonBlank(categoryOrEmpty, "").trim();
        if (category.isBlank()) {
            category = guessCategoryForGame(name);
        }
        if (name.isBlank()) {
            return "Donne-moi le nom du jeu (champ Nom) pour que je fasse un texte marketing (2 lignes).\n";
        }
        String catPart = category.isBlank() ? "un style de jeu" : ("la categorie " + category);
        return """
                Presentation (2 lignes):
                %s te plonge dans %s avec un gameplay dynamique et accessible.
                Ideal pour jouer entre amis, progresser et relever des defis competitifs.
                """.formatted(name, catPart);
    }

    private static String buildDifference(String msgOneLine) {
        String[] quoted = extractQuotedPair(msgOneLine);
        String a = quoted[0];
        String b = quoted[1];
        if (a.isBlank() || b.isBlank()) {
            return """
                    Pour comparer, ecris par exemple:
                    - "Difference entre \"FPS\" et \"Battle Royale\""
                    """;
        }
        String al = a.toLowerCase();
        String bl = b.toLowerCase();
        if ((al.contains("fps") && bl.contains("battle")) || (bl.contains("fps") && al.contains("battle"))) {
            return """
                    Difference FPS vs Battle Royale:
                    - FPS: genre "tir" (vue 1ere personne), modes souvent match-based (team deathmatch, etc.).
                    - Battle Royale: mode/genre "survie" avec loot + zone qui retrecit + dernier survivant/equipe.
                    Astuce: tu peux avoir un jeu FPS qui a aussi un mode Battle Royale.
                    """;
        }
        return "Difference entre \"" + a + "\" et \"" + b + "\":\n- " + a + ": concept/genre a definir\n- " + b + ": concept/genre a definir\n";
    }

    private static String guessCategoryForGame(String name) {
        if (name == null) {
            return "";
        }
        String n = name.trim().toLowerCase();
        if (n.isBlank()) {
            return "";
        }
        if (n.contains("league of legends") || n.contains("dota")) return "MOBA";
        if (n.contains("valorant") || n.contains("counter") || n.contains("call of duty")) return "FPS";
        if (n.contains("warzone") || n.contains("fortnite") || n.contains("pubg")) return "Battle Royale";
        if (n.contains("fifa") || n.contains("nba") || n.contains("pes")) return "Sports";
        if (n.contains("minecraft")) return "Sandbox";
        if (n.contains("starcraft") || n.contains("age of empires")) return "RTS";
        return "";
    }

    private static String[] extractQuotedPair(String value) {
        String[] out = new String[]{"", ""};
        if (value == null) {
            return out;
        }
        int first = value.indexOf('"');
        if (first < 0) {
            return out;
        }
        int second = value.indexOf('"', first + 1);
        if (second < 0) {
            return out;
        }
        out[0] = value.substring(first + 1, second).trim();
        int third = value.indexOf('"', second + 1);
        if (third < 0) {
            return out;
        }
        int fourth = value.indexOf('"', third + 1);
        if (fourth < 0) {
            return out;
        }
        out[1] = value.substring(third + 1, fourth).trim();
        return out;
    }

    private static String statusSuggestion(String msgLower) {
        if (msgLower.contains("public") || msgLower.contains("publier") || msgLower.contains("published")) {
            return "Statut conseille: PUBLISHED (visible/public).\n";
        }
        return "Statut conseille: DRAFT (brouillon) tant que tu n'es pas pret a publier.\n";
    }

    private static String extractQuoted(String value) {
        if (value == null) {
            return "";
        }
        int first = value.indexOf('"');
        if (first < 0) {
            return "";
        }
        int second = value.indexOf('"', first + 1);
        if (second < 0) {
            return "";
        }
        return value.substring(first + 1, second).trim();
    }

    private static String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private static String firstNonBlank(String value, String fallback) {
        if (value != null && !value.isBlank()) {
            return value;
        }
        return fallback == null ? "" : fallback;
    }

    private static String slugify(String value) {
        if (value == null) {
            return "";
        }
        String out = value.trim().toLowerCase();
        out = out.replaceAll("[^a-z0-9]+", "-");
        out = out.replaceAll("-{2,}", "-");
        out = out.replaceAll("^-+", "");
        out = out.replaceAll("-+$", "");
        return out;
    }

    private static final class GameHandler implements HttpHandler {
        private final GameRepository gameRepo = new GameRepository();
        private final ActivityLogService activityLogService = new ActivityLogService();

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
            String key = tail(exchange, "/games/");
            if (key.isBlank()) {
                sendNotFound(exchange, "Jeu non specifie.");
                return;
            }

            GameModel game;
            Integer id = parseIntOrNull(key);
            try {
                if (id != null) {
                    game = gameRepo.findById(id);
                } else {
                    game = gameRepo.findBySlug(key);
                }
            } catch (Exception ex) {
                sendHtml(exchange, 500, page("Erreur", "<p>Erreur DB: " + escapeHtml(ex.getMessage()) + "</p>"));
                return;
            }

            if (game == null) {
                sendNotFound(exchange, "Jeu introuvable: " + key);
                return;
            }

            try {
                if (game.getGameId() != null) {
                    int updated = gameRepo.incrementViewsCount(game.getGameId());
                    game.setViewsCount(updated);
                    activityLogService.log("GAME", "VIEW", game.getGameId());
                }
            } catch (Exception ignored) {
            }

            String pageUrl = deviceBaseUrl() == null ? "" : deviceBaseUrl() + "/games/" + escapeHtml(key);
            String mediaPath = game.getCoverName();
            String mediaExternal = ImageResolver.toExternalForm(mediaPath);
            String mediaTag = "";
            if (mediaExternal != null) {
                // Convert file path to server-served media when possible (so phone can load it).
                String normalized = mediaPath == null ? "" : mediaPath.replace('\\', '/');
                if (!normalized.isBlank()) {
                    mediaTag = "<div class=\"media\"><img alt=\"cover\" src=\"/media?path=" + urlEncode(normalized) + "\"/></div>";
                }
            }

            String catKey = game.getCategoryId() == null ? "" : game.getCategoryId().toString();
            String body = """
                    %s
                    <h1>%s</h1>
                    <p class="muted">Jeu video</p>
                    <div class="chips">
                      <span class="chip">ID: %d</span>
                      <span class="chip">Categorie: %s</span>
                      <span class="chip">Slug: %s</span>
                      <span class="chip">Statut: %s</span>
                    </div>
                    <p style="margin-top:12px" class="muted">Editeur: %s</p>
                    <p style="margin-top:12px">%s</p>
                    <div class="actions">
                      <button class="btn btn--primary" onclick="copyText('%s')">Copier le lien</button>
                      <button class="btn" onclick="copyText('%s')">Copier le slug</button>
                      <a class="btn" href="/categories/%s">Voir categorie</a>
                      <a class="btn" href="/">Accueil</a>
                    </div>
                    """.formatted(
                    mediaTag,
                    escapeHtml(game.getName()),
                    game.getGameId() == null ? 0 : game.getGameId(),
                    escapeHtml(game.getCategoryName()),
                    escapeHtml(game.getSlug()),
                    escapeHtml(game.getStatus()),
                    escapeHtml(game.getPublisher()),
                    escapeHtml(game.getDescription()),
                    escapeJs(pageUrl),
                    escapeJs(game.getSlug()),
                    escapeHtml(catKey)
            );
            sendHtml(exchange, 200, page("Jeu - " + game.getName(), body));
            } catch (Exception ex) {
                sendHtml(exchange, 500, page("Erreur", "<p>Erreur serveur.</p><pre>" + escapeHtml(ex.getMessage()) + "</pre>"));
            }
        }
    }

    private static final class CategoryHandler implements HttpHandler {
        private final CategoryRepository categoryRepo = new CategoryRepository();
        private final GameRepository gameRepo = new GameRepository();

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
            String key = tail(exchange, "/categories/");
            if (key.isBlank()) {
                sendNotFound(exchange, "Categorie non specifiee.");
                return;
            }

            CategoryModel category;
            Integer id = parseIntOrNull(key);
            try {
                if (id != null) {
                    category = categoryRepo.findById(id);
                } else {
                    category = categoryRepo.findBySlug(key);
                }
            } catch (Exception ex) {
                sendHtml(exchange, 500, page("Erreur", "<p>Erreur DB: " + escapeHtml(ex.getMessage()) + "</p>"));
                return;
            }

            if (category == null) {
                sendNotFound(exchange, "Categorie introuvable: " + key);
                return;
            }

            String pageUrl = deviceBaseUrl() == null ? "" : deviceBaseUrl() + "/categories/" + escapeHtml(key);
            int gameCount = 0;
            String gamesListHtml = "";
            if (category.getCategoryId() != null) {
                try {
                    var games = gameRepo.findByCategoryId(category.getCategoryId());
                    gameCount = games.size();
                    int max = Math.min(8, games.size());
                    StringBuilder list = new StringBuilder();
                    list.append("<div class=\"list\">");
                    for (int i = 0; i < max; i++) {
                        GameModel g = games.get(i);
                        String gKey = g.getSlug() != null && !g.getSlug().isBlank()
                                ? g.getSlug()
                                : (g.getGameId() == null ? "" : g.getGameId().toString());
                        list.append("<div class=\"item\">")
                                .append("<div class=\"item-title\">")
                                .append("<a href=\"/games/").append(urlEncode(gKey)).append("\">")
                                .append(escapeHtml(g.getName()))
                                .append("</a>")
                                .append("</div>")
                                .append("<div class=\"item-meta\">Slug: ")
                                .append(escapeHtml(g.getSlug()))
                                .append(" | Statut: ")
                                .append(escapeHtml(g.getStatus()))
                                .append("</div>")
                                .append("</div>");
                    }
                    list.append("</div>");
                    gamesListHtml = "<p class=\"muted\" style=\"margin-top:12px\">Jeux ("
                            + gameCount + ")</p>" + list;
                } catch (Exception ignored) {
                    gamesListHtml = "<p class=\"muted\" style=\"margin-top:12px\">Jeux: indisponible</p>";
                }
            }

            String body = """
                    <h1>%s</h1>
                    <p class="muted">Categorie</p>
                    <div class="chips">
                      <span class="chip">ID: %d</span>
                      <span class="chip">Slug: %s</span>
                    </div>
                    <p style="margin-top:12px">%s</p>
                    %s
                    <div class="actions">
                      <button class="btn btn--primary" onclick="copyText('%s')">Copier le lien</button>
                      <button class="btn" onclick="copyText('%s')">Copier le slug</button>
                      <a class="btn" href="/">Accueil</a>
                    </div>
                    """.formatted(
                    escapeHtml(category.getName()),
                    category.getCategoryId() == null ? 0 : category.getCategoryId(),
                    escapeHtml(category.getSlug()),
                    escapeHtml(category.getDescription()),
                    gamesListHtml,
                    escapeJs(pageUrl),
                    escapeJs(category.getSlug())
            );
            sendHtml(exchange, 200, page("Categorie - " + category.getName(), body));
            } catch (Exception ex) {
                sendHtml(exchange, 500, page("Erreur", "<p>Erreur serveur.</p><pre>" + escapeHtml(ex.getMessage()) + "</pre>"));
            }
        }
    }

    private static String urlEncode(String value) {
        if (value == null) {
            return "";
        }
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String escapeJs(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
