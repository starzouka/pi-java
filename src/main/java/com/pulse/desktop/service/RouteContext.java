package com.pulse.desktop.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RouteContext {
    public static final String KEY_TOURNAMENT_ID = "tournament_id";
    public static final String KEY_MATCH_ID = "match_id";
    public static final String KEY_REQUEST_ID = "request_id";
    public static final String KEY_USER_ID = "user_id";
    public static final String KEY_TEAM_ID = "team_id";
    public static final String KEY_TEAM_MODE = "team_mode";
    public static final String KEY_PRODUCT_ID = "product_id";
    public static final String KEY_ORDER_ID = "order_id";
    public static final String KEY_CART_ID = "cart_id";
    public static final String KEY_GAME_ID = "game_id";

    private static final Map<String, Object> VALUES = new ConcurrentHashMap<>();

    private RouteContext() {
    }

    public static void putInt(String key, Integer value) {
        if (key == null || key.isBlank()) {
            return;
        }
        if (value == null) {
            VALUES.remove(key);
            return;
        }
        VALUES.put(key, value);
    }

    public static Integer getInt(String key) {
        Object value = VALUES.get(key);
        if (value instanceof Integer intValue) {
            return intValue;
        }
        return null;
    }

    public static void putString(String key, String value) {
        if (key == null || key.isBlank()) {
            return;
        }
        if (value == null || value.isBlank()) {
            VALUES.remove(key);
            return;
        }
        VALUES.put(key, value.trim());
    }

    public static String getString(String key) {
        Object value = VALUES.get(key);
        if (value instanceof String stringValue) {
            return stringValue;
        }
        return null;
    }

    public static void clear(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        VALUES.remove(key);
    }

    public static void clearAll() {
        VALUES.clear();
    }
}
