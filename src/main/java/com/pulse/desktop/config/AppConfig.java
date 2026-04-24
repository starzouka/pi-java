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
        return envOrPropOrSymfony("PULSE_DB_HOST", "db.host", "PULSE_DB_HOST", "127.0.0.1");
    }

    public static int dbPort() {
        String value = envOrPropOrSymfony("PULSE_DB_PORT", "db.port", "PULSE_DB_PORT", "3306");
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return 3306;
        }
    }

    public static String dbName() {
        return envOrPropOrSymfony("PULSE_DB_NAME", "db.name", "PULSE_DB_NAME", "pulsedb");
    }

    public static String dbUser() {
        return envOrPropOrSymfony("PULSE_DB_USER", "db.user", "PULSE_DB_USER", "root");
    }

    public static String dbPassword() {
        return envOrPropOrSymfony("PULSE_DB_PASSWORD", "db.password", "PULSE_DB_PASSWORD", "");
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

    public static String aiEndpoint() {
        return envOrProp("PULSE_AI_ENDPOINT", "ai.endpoint", "");
    }

    public static String aiApiKey() {
        return envOrProp("PULSE_AI_API_KEY", "ai.api-key", "");
    }

    public static String qrBaseUrl() {
        return envOrProp("PULSE_QR_BASE_URL", "qr.base-url", webBaseUrl());
    }

    public static boolean qrServerEnabled() {
        String raw = envOrProp("PULSE_QR_SERVER_ENABLED", "qr.server.enabled", "true");
        return "true".equalsIgnoreCase(raw.trim()) || "1".equals(raw.trim());
    }

    public static int qrServerPort() {
        String raw = envOrProp("PULSE_QR_SERVER_PORT", "qr.server.port", "8000");
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            return 8000;
        }
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

    private static String prop(String key, String defaultValue) {
        String value = PROPS.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
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
