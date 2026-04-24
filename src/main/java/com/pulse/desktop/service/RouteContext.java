package com.pulse.desktop.service;

public final class RouteContext {
    private static Integer selectedGameId;
    private static String pendingResetPasswordToken;

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

    public static String getPendingResetPasswordToken() {
        return pendingResetPasswordToken;
    }

    public static void setPendingResetPasswordToken(String token) {
        pendingResetPasswordToken = token;
    }

    public static void clearPendingResetPasswordToken() {
        pendingResetPasswordToken = null;
    }
}

