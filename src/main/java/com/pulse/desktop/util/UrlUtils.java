package com.pulse.desktop.util;

import java.net.URI;

public final class UrlUtils {
    private UrlUtils() {
    }

    public static String toDeviceAccessibleBaseUrl(String configuredBaseUrl) {
        String base = trimTrailingSlash(configuredBaseUrl);
        URI uri;
        try {
            uri = URI.create(base);
        } catch (Exception ignored) {
            return base;
        }

        String host = uri.getHost();
        if (host == null) {
            return base;
        }
        if (!"127.0.0.1".equals(host) && !"localhost".equalsIgnoreCase(host)) {
            return base;
        }

        String lanIp = NetworkUtils.findLanIPv4Address();
        if (lanIp == null || lanIp.isBlank()) {
            return base;
        }

        try {
            URI replaced = new URI(
                    uri.getScheme(),
                    uri.getUserInfo(),
                    lanIp,
                    uri.getPort(),
                    uri.getPath(),
                    uri.getQuery(),
                    uri.getFragment()
            );
            return trimTrailingSlash(replaced.toString());
        } catch (Exception ignored) {
            return base;
        }
    }

    public static String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://127.0.0.1:8000";
        }
        String out = value.trim();
        while (out.endsWith("/")) {
            out = out.substring(0, out.length() - 1);
        }
        return out;
    }
}

