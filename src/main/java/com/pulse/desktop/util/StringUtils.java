package com.pulse.desktop.util;

import java.util.Locale;

/**
 * Centralized utility methods for string operations.
 */
public final class StringUtils {
    private StringUtils() {
    }

    /**
     * Returns trimmed string or empty string if null/blank.
     */
    public static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * Normalizes text: trims and handles null.
     */
    public static String normalizeText(String value) {
        return safe(value);
    }

    /**
     * Returns null if string is blank after trim, otherwise returns trimmed value.
     */
    public static String nullIfBlank(String value) {
        String v = safe(value);
        return v.isBlank() ? null : v;
    }

    /**
     * Returns first non-blank string, or fallback if both are blank.
     */
    public static String firstNonBlank(String first, String fallback) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return fallback;
    }

    /**
     * Returns fallback if value is empty or null.
     */
    public static String defaultIfBlank(String value, String fallback) {
        String v = safe(value);
        return v.isBlank() ? fallback : v;
    }

    /**
     * Trims and lowercases string.
     */
    public static String toLowerCase(String value) {
        return safe(value).toLowerCase(Locale.ROOT);
    }

    /**
     * Trims and uppercases string.
     */
    public static String toUpperCase(String value) {
        return safe(value).toUpperCase(Locale.ROOT);
    }
}

