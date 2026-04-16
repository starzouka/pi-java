package com.pulse.desktop.auth;

public class SessionUser {
    private final int userId;
    private final String username;
    private final String email;
    private final String role;
    private final String displayName;
    private final boolean emailVerified;
    private final boolean twoFactorEnabled;

    public SessionUser(int userId, String username, String email, String role, String displayName) {
        this(userId, username, email, role, displayName, false, false);
    }

    public SessionUser(
            int userId,
            String username,
            String email,
            String role,
            String displayName,
            boolean emailVerified,
            boolean twoFactorEnabled
    ) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.role = role;
        this.displayName = displayName;
        this.emailVerified = emailVerified;
        this.twoFactorEnabled = twoFactorEnabled;
    }

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public boolean isTwoFactorEnabled() {
        return twoFactorEnabled;
    }
}
