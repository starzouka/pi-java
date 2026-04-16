package com.pulse.desktop.service;

public final class RouteContext {
    private static Integer selectedGameId;

    private RouteContext() {
    }

    public static Integer getSelectedGameId() {
        return selectedGameId;
    }

    public static void setSelectedGameId(Integer selectedGameId) {
        RouteContext.selectedGameId = selectedGameId;
    }

    public static void clearSelectedGameId() {
        selectedGameId = null;
    }
}

