package com.pulse.desktop.model;

import com.pulse.desktop.auth.SessionUser;

public record AuthLoginResult(Status status, SessionUser user) {
    public enum Status {
        SUCCESS,
        INVALID_CREDENTIALS,
        ACCOUNT_INACTIVE,
        EMAIL_NOT_VERIFIED,
        TWO_FACTOR_REQUIRED
    }

    public static AuthLoginResult success(SessionUser user) {
        return new AuthLoginResult(Status.SUCCESS, user);
    }

    public static AuthLoginResult failure(Status status) {
        return new AuthLoginResult(status, null);
    }
}
