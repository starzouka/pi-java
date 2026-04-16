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
