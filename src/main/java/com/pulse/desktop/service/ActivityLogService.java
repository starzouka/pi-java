package com.pulse.desktop.service;

import com.pulse.desktop.auth.SessionContext;
import com.pulse.desktop.auth.SessionUser;
import com.pulse.desktop.repo.ActivityLogRepository;

public class ActivityLogService {
    private final ActivityLogRepository repo = new ActivityLogRepository();

    public void log(String entityType, String action, Integer entityId) {
        try {
            SessionUser user = SessionContext.getCurrentUser();
            Integer actorUserId = user == null ? null : user.getUserId();
            String actorRole = SessionContext.currentRole();
            String actorEmail = user == null ? null : user.getEmail();
            repo.insert(entityType, action, entityId, actorUserId, actorRole, actorEmail);
        } catch (Exception ignored) {
            // Logging must never break UX.
        }
    }
}

