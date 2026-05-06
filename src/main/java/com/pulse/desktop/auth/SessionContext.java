package com.pulse.desktop.auth;

public final class SessionContext {
    private static SessionUser currentUser;
    private static SessionUser pendingTwoFactorUser;
    private static String pendingTwoFactorSetupSecret;

    private SessionContext() {
    }

    public static SessionUser getCurrentUser() {
        return currentUser;
    }

    public static boolean isAuthenticated() {
        return currentUser != null;
    }

    public static String currentRole() {
        if (currentUser == null || currentUser.getRole() == null || currentUser.getRole().isBlank()) {
            return "GUEST";
        }
        return currentUser.getRole().trim().toUpperCase();
    }

    public static void login(SessionUser user) {
        currentUser = user;
        pendingTwoFactorUser = null;
    }

    public static void logout() {
        currentUser = null;
        pendingTwoFactorUser = null;
        pendingTwoFactorSetupSecret = null;
    }

    public static String currentDisplayName() {
        return currentUser == null ? "Invite" : currentUser.getDisplayName();
    }

    public static void beginTwoFactor(SessionUser user) {
        pendingTwoFactorUser = user;
    }

    public static SessionUser getPendingTwoFactorUser() {
        return pendingTwoFactorUser;
    }

    public static boolean hasPendingTwoFactor() {
        return pendingTwoFactorUser != null;
    }

    public static void clearPendingTwoFactor() {
        pendingTwoFactorUser = null;
    }

    public static void setPendingTwoFactorSetupSecret(String secret) {
        pendingTwoFactorSetupSecret = (secret == null || secret.isBlank()) ? null : secret.trim();
    }

    public static String getPendingTwoFactorSetupSecret() {
        return pendingTwoFactorSetupSecret;
    }

    public static void clearPendingTwoFactorSetupSecret() {
        pendingTwoFactorSetupSecret = null;
    }
}
