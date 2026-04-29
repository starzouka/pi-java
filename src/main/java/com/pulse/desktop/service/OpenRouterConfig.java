package com.pulse.desktop.service;

import com.pulse.desktop.config.AppConfig;

public record OpenRouterConfig(
        boolean enabled,
        String apiKey,
        String baseUrl,
        String model,
        String appName,
        String siteUrl,
        int timeoutSeconds
) {
    public static OpenRouterConfig load() {
        String apiKey = safe(AppConfig.openRouterApiKey());
        String baseUrl = safe(AppConfig.openRouterBaseUrl());
        String model = safe(AppConfig.openRouterModel());
        String appName = safe(AppConfig.openRouterAppName());
        String siteUrl = safe(AppConfig.openRouterSiteUrl());
        if (appName.isBlank()) {
            appName = AppConfig.appName();
        }
        return new OpenRouterConfig(
                !apiKey.isBlank(),
                apiKey,
                baseUrl.isBlank() ? "https://openrouter.ai/api/v1/chat/completions" : baseUrl,
                model.isBlank() ? "openai/gpt-4o-mini" : model,
                appName,
                siteUrl.isBlank() ? "http://127.0.0.1" : siteUrl,
                30
        );
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}

