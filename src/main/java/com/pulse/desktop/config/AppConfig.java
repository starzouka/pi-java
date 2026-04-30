package com.pulse.desktop.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public final class AppConfig {
    private static final Properties PROPS = loadProperties();

    private AppConfig() {
    }

    public static String appName() {
        return prop("app.name", "PULSE Desktop JavaFX");
    }

    public static String dbHost() {
        return envOrProp("PULSE_DB_HOST", "db.host", "127.0.0.1");
    }

    public static int dbPort() {
        String value = envOrProp("PULSE_DB_PORT", "db.port", "3306");
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return 3306;
        }
    }

    public static String dbName() {
        return envOrProp("PULSE_DB_NAME", "db.name", "pulsedb");
    }

    public static String dbUser() {
        return envOrProp("PULSE_DB_USER", "db.user", "root");
    }

    public static String dbPassword() {
        return envOrProp("PULSE_DB_PASSWORD", "db.password", "");
    }

    public static String jdbcUrl() {
        String params = prop("db.params", "useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        return "jdbc:mysql://%s:%d/%s?%s".formatted(dbHost(), dbPort(), dbName(), params);
    }

    public static String webRoot() {
        return envOrProp("PULSE_WEB_ROOT", "app.web.root", "..");
    }

    public static String webBaseUrl() {
        return envOrPropOrSymfony("PULSE_WEB_BASE_URL", "app.web.base-url", "APP_URL", "http://127.0.0.1:8000");
    }

    public static String webPublicBaseUrl() {
        return envOrPropOrSymfonyWithAliases(
                "PULSE_WEB_PUBLIC_BASE_URL",
                "app.web.public-base-url",
                webBaseUrl(),
                "APP_PUBLIC_URL"
        );
    }

    public static String appSecret() {
        return envOrPropOrSymfony("PULSE_APP_SECRET", "app.secret", "APP_SECRET", "");
    }

    public static String mailerDsn() {
        return envOrPropOrSymfony("PULSE_MAILER_DSN", "mail.dsn", "MAILER_DSN", "null://null");
    }

    public static String mailerFromAddress() {
        return envOrPropOrSymfony("PULSE_MAILER_FROM_ADDRESS", "mail.from.address", "MAILER_FROM_ADDRESS", "no-reply@pulse.local");
    }

    public static int verifyEmailLifetimeSeconds() {
        String raw = envOrProp("PULSE_VERIFY_EMAIL_LIFETIME", "verify.email.lifetime.seconds", "86400");
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            return 86400;
        }
    }

    public static int resetPasswordLifetimeSeconds() {
        String raw = envOrProp("PULSE_RESET_PASSWORD_LIFETIME", "reset.password.lifetime.seconds", "3600");
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            return 3600;
        }
    }

    public static String ollamaBaseUrl() {
        return envOrProp("PULSE_OLLAMA_URL", "ai.ollama.url", "http://127.0.0.1:11434");
    }

    public static String ollamaModel() {
        return envOrProp("PULSE_OLLAMA_MODEL", "ai.ollama.model", "gemma3:4b");
    }

    public static int ollamaTimeoutSeconds() {
        String raw = envOrProp("PULSE_OLLAMA_TIMEOUT", "ai.ollama.timeout.seconds", "60");
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            return 60;
        }
    }

    public static boolean openRouterEnabled() {
        String raw = envOrProp("PULSE_OPENROUTER_ENABLED", "ai.openrouter.enabled", "true");
        String value = raw == null ? "" : raw.trim().toLowerCase();
        return !"false".equals(value) && !"0".equals(value) && !"no".equals(value);
    }

    public static String openRouterBaseUrl() {
        return envOrProp("PULSE_OPENROUTER_BASE_URL", "ai.openrouter.base-url", "https://openrouter.ai/api/v1");
    }

    public static String openRouterApiKey() {
        return envOrProp("PULSE_OPENROUTER_API_KEY", "ai.openrouter.api-key", "");
    }

    public static String openRouterModel() {
        return envOrProp("PULSE_OPENROUTER_MODEL", "ai.openrouter.model", "openai/gpt-4o-mini");
    }

    public static int openRouterTimeoutSeconds() {
        String raw = envOrProp("PULSE_OPENROUTER_TIMEOUT", "ai.openrouter.timeout.seconds", "60");
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            return 60;
        }
    }

    public static String openRouterHttpReferer() {
        return envOrProp("PULSE_OPENROUTER_HTTP_REFERER", "ai.openrouter.http-referer", "http://localhost");
    }

    public static String openRouterAppTitle() {
        return envOrProp("PULSE_OPENROUTER_APP_TITLE", "ai.openrouter.app-title", appName());
    }

    public static boolean tournamentNotificationsEnabled() {
        String raw = envOrProp("PULSE_TOURNAMENT_NOTIF_ENABLED", "notifications.tournament.enabled", "true");
        String value = raw == null ? "" : raw.trim().toLowerCase();
        return !"false".equals(value) && !"0".equals(value) && !"no".equals(value);
    }

    public static int tournamentNotificationTaskIntervalMinutes() {
        String raw = envOrProp("PULSE_TOURNAMENT_NOTIF_INTERVAL_MINUTES", "notifications.tournament.task.interval.minutes", "1");
        try {
            return Math.max(1, Math.min(60, Integer.parseInt(raw)));
        } catch (NumberFormatException ex) {
            return 1;
        }
    }

    public static String tournamentNotificationTaskName() {
        return envOrProp("PULSE_TOURNAMENT_NOTIF_TASK_NAME", "notifications.tournament.task.name", "PULSE Tournament Notifier");
    }

    public static boolean teamDiscordNotificationsEnabled() {
        String raw = envOrProp("PULSE_TEAM_DISCORD_ENABLED", "notifications.team.discord.enabled", "false");
        String value = raw == null ? "" : raw.trim().toLowerCase();
        return !"false".equals(value) && !"0".equals(value) && !"no".equals(value);
    }

    public static String teamDiscordWebhookUrl() {
        return envOrProp("PULSE_TEAM_DISCORD_WEBHOOK_URL", "notifications.team.discord.webhook-url", "");
    }

    public static String teamDiscordWebhookUsername() {
        return envOrProp("PULSE_TEAM_DISCORD_WEBHOOK_USERNAME", "notifications.team.discord.username", "PULSE Team Bot");
    }

    public static String teamDiscordWebhookAvatarUrl() {
        return envOrProp("PULSE_TEAM_DISCORD_WEBHOOK_AVATAR_URL", "notifications.team.discord.avatar-url", "");
    }

    public static boolean googleSignInEnabled() {
        String raw = envOrProp("PULSE_GOOGLE_SIGNIN_ENABLED", "auth.google.enabled", "true");
        String value = raw == null ? "" : raw.trim().toLowerCase();
        return !"false".equals(value) && !"0".equals(value) && !"no".equals(value);
    }

    public static String googleClientId() {
        return envOrPropOrSymfonyWithAliases(
                "PULSE_GOOGLE_CLIENT_ID",
                "auth.google.client-id",
                "",
                "GOOGLE_CLIENT_ID",
                "GOOGLE_OAUTH_CLIENT_ID",
                "GOOGLE_AUTH_CLIENT_ID",
                "GOOGLE_DESKTOP_CLIENT_ID"
        );
    }

    public static String googleClientSecret() {
        return envOrPropOrSymfonyWithAliases(
                "PULSE_GOOGLE_CLIENT_SECRET",
                "auth.google.client-secret",
                "",
                "GOOGLE_CLIENT_SECRET",
                "GOOGLE_OAUTH_CLIENT_SECRET",
                "GOOGLE_AUTH_CLIENT_SECRET",
                "GOOGLE_DESKTOP_CLIENT_SECRET"
        );
    }

    public static String googleRedirectHost() {
        return envOrProp("PULSE_GOOGLE_REDIRECT_HOST", "auth.google.redirect.host", "127.0.0.1");
    }

    public static int googleRedirectPort() {
        String raw = envOrProp("PULSE_GOOGLE_REDIRECT_PORT", "auth.google.redirect.port", "53682");
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            return 53682;
        }
    }

    public static String googleRedirectPath() {
        String path = envOrProp("PULSE_GOOGLE_REDIRECT_PATH", "auth.google.redirect.path", "/oauth2/callback");
        if (path == null || path.isBlank()) {
            return "/oauth2/callback";
        }
        return path.startsWith("/") ? path.trim() : "/" + path.trim();
    }

    public static int googleSignInTimeoutSeconds() {
        String raw = envOrProp("PULSE_GOOGLE_SIGNIN_TIMEOUT", "auth.google.timeout.seconds", "180");
        try {
            return Math.max(30, Integer.parseInt(raw));
        } catch (NumberFormatException ex) {
            return 180;
        }
    }

    public static boolean steamSignInEnabled() {
        String raw = envOrProp("PULSE_STEAM_SIGNIN_ENABLED", "auth.steam.enabled", "true");
        String value = raw == null ? "" : raw.trim().toLowerCase();
        return !"false".equals(value) && !"0".equals(value) && !"no".equals(value);
    }

    public static String steamApiKey() {
        return envOrPropOrSymfonyWithAliases(
                "PULSE_STEAM_API_KEY",
                "auth.steam.api-key",
                "",
                "STEAM_API_KEY",
                "STEAM_WEB_API_KEY"
        );
    }

    public static String steamRedirectHost() {
        return envOrProp("PULSE_STEAM_REDIRECT_HOST", "auth.steam.redirect.host", "127.0.0.1");
    }

    public static int steamRedirectPort() {
        String raw = envOrProp("PULSE_STEAM_REDIRECT_PORT", "auth.steam.redirect.port", "53683");
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            return 53683;
        }
    }

    public static String steamRedirectPath() {
        String path = envOrProp("PULSE_STEAM_REDIRECT_PATH", "auth.steam.redirect.path", "/steam/callback");
        if (path == null || path.isBlank()) {
            return "/steam/callback";
        }
        return path.startsWith("/") ? path.trim() : "/" + path.trim();
    }

    public static int steamSignInTimeoutSeconds() {
        String raw = envOrProp("PULSE_STEAM_SIGNIN_TIMEOUT", "auth.steam.timeout.seconds", "180");
        try {
            return Math.max(30, Integer.parseInt(raw));
        } catch (NumberFormatException ex) {
            return 180;
        }
    }

    public static boolean recaptchaEnabled() {
        String raw = envOrProp("PULSE_RECAPTCHA_ENABLED", "security.recaptcha.enabled", "true");
        String value = raw == null ? "" : raw.trim().toLowerCase();
        return !"false".equals(value) && !"0".equals(value) && !"no".equals(value);
    }

    public static String recaptchaSiteKey() {
        String configured = envOrProp(
                "PULSE_RECAPTCHA_SITE_KEY",
                "security.recaptcha.site-key",
                "6LeIxAcTAAAAAJcZVRqyHh71UMIEGNQ_MXjiZKhI"
        );
        if (isGoogleRecaptchaV2TestSiteKey(configured)) {
            String symfony = SymfonyEnv.get("RECAPTCHA_SITE_KEY");
            if (symfony != null && !symfony.isBlank()) {
                return symfony.trim();
            }
        }
        return configured;
    }

    public static String recaptchaSecretKey() {
        String configured = envOrProp(
                "PULSE_RECAPTCHA_SECRET_KEY",
                "security.recaptcha.secret-key",
                "6LeIxAcTAAAAAGG-vFI1TnRWxMZNFuojJ4WifJWe"
        );
        if (isGoogleRecaptchaV2TestSecretKey(configured)) {
            String symfony = SymfonyEnv.get("RECAPTCHA_SECRET_KEY");
            if (symfony != null && !symfony.isBlank()) {
                return symfony.trim();
            }
        }
        return configured;
    }

    public static String recaptchaVerifyUrl() {
        return envOrProp("PULSE_RECAPTCHA_VERIFY_URL", "security.recaptcha.verify-url", "https://www.google.com/recaptcha/api/siteverify");
    }

    public static Path webRootPath() {
        Path configured = Paths.get(webRoot());
        if (!configured.isAbsolute()) {
            configured = Paths.get("").toAbsolutePath().resolve(configured).normalize();
        }
        return configured;
    }

    private static String envOrProp(String envName, String propName, String defaultValue) {
        String envValue = System.getenv(envName);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }
        return prop(propName, defaultValue);
    }

    private static String envOrPropOrSymfony(String envName, String propName, String symfonyVarName, String defaultValue) {
        String envValue = System.getenv(envName);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }

        String property = PROPS.getProperty(propName);
        if (property != null && !property.isBlank()) {
            return property.trim();
        }

        String symfonyValue = SymfonyEnv.get(symfonyVarName);
        if (symfonyValue != null && !symfonyValue.isBlank()) {
            return symfonyValue.trim();
        }

        return defaultValue;
    }

    private static String envOrPropOrSymfonyWithAliases(
            String envName,
            String propName,
            String defaultValue,
            String... symfonyAliases
    ) {
        String envValue = System.getenv(envName);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }

        String property = PROPS.getProperty(propName);
        if (property != null && !property.isBlank()) {
            return property.trim();
        }

        if (symfonyAliases != null) {
            for (String alias : symfonyAliases) {
                if (alias == null || alias.isBlank()) {
                    continue;
                }
                String value = SymfonyEnv.get(alias.trim());
                if (value != null && !value.isBlank()) {
                    return value.trim();
                }
            }
        }

        return defaultValue;
    }

    private static String prop(String key, String defaultValue) {
        String value = PROPS.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

    private static boolean isGoogleRecaptchaV2TestSiteKey(String key) {
        if (key == null) {
            return false;
        }
        return "6LeIxAcTAAAAAJcZVRqyHh71UMIEGNQ_MXjiZKhI".equals(key.trim());
    }

    private static boolean isGoogleRecaptchaV2TestSecretKey(String key) {
        if (key == null) {
            return false;
        }
        return "6LeIxAcTAAAAAGG-vFI1TnRWxMZNFuojJ4WifJWe".equals(key.trim());
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream input = AppConfig.class.getResourceAsStream("/application.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException ignored) {
            // Keep defaults if file cannot be read.
        }
        return properties;
    }
}
