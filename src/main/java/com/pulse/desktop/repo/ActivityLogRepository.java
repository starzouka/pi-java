package com.pulse.desktop.repo;

import com.pulse.desktop.db.Jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class ActivityLogRepository {
    private static volatile boolean schemaReady;

    public void ensureSchema() throws SQLException {
        if (schemaReady) {
            return;
        }
        synchronized (ActivityLogRepository.class) {
            if (schemaReady) {
                return;
            }
            try (Connection connection = Jdbc.open();
                 Statement statement = connection.createStatement()) {
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS pulse_activity_log (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            entity_type VARCHAR(32) NOT NULL,
                            action VARCHAR(32) NOT NULL,
                            entity_id INT NULL,
                            actor_user_id INT NULL,
                            actor_role VARCHAR(32) NULL,
                            actor_email VARCHAR(255) NULL,
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            INDEX idx_pulse_activity_log_created_at (created_at),
                            INDEX idx_pulse_activity_log_action (action),
                            INDEX idx_pulse_activity_log_entity (entity_type, entity_id)
                        )
                        """);
            }
            schemaReady = true;
        }
    }

    public void insert(
            String entityType,
            String action,
            Integer entityId,
            Integer actorUserId,
            String actorRole,
            String actorEmail
    ) throws SQLException {
        ensureSchema();
        String sql = """
                INSERT INTO pulse_activity_log (entity_type, action, entity_id, actor_user_id, actor_role, actor_email)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, entityType);
            statement.setString(2, action);
            if (entityId == null) {
                statement.setNull(3, java.sql.Types.INTEGER);
            } else {
                statement.setInt(3, entityId);
            }
            if (actorUserId == null) {
                statement.setNull(4, java.sql.Types.INTEGER);
            } else {
                statement.setInt(4, actorUserId);
            }
            statement.setString(5, actorRole);
            statement.setString(6, actorEmail);
            statement.executeUpdate();
        }
    }
}

