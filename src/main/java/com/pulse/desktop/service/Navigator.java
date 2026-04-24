package com.pulse.desktop.service;

import java.util.function.Consumer;

public final class Navigator {
    private static Consumer<String> routeConsumer = route -> {
    };
    private static Runnable authConsumer = () -> {
    };

    private Navigator() {
    }

    public static void init(Consumer<String> routeConsumer, Runnable authConsumer) {
        Navigator.routeConsumer = routeConsumer;
        Navigator.authConsumer = authConsumer;
    }

    public static void goTo(String route) {
        routeConsumer.accept(route);
    }

    public static void authChanged() {
        authConsumer.run();
    }
}