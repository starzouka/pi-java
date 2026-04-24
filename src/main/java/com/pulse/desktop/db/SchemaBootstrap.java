package com.pulse.desktop.db;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

final class SchemaBootstrap {
    private static final AtomicBoolean ENSURED = new AtomicBoolean(false);

    private SchemaBootstrap() {
    }

    static void ensure(Connection connection) throws SQLException {
        if (ENSURED.get()) {
            return;
        }
        synchronized (SchemaBootstrap.class) {
            if (ENSURED.get()) {
                return;
            }
            ensureInternal(Objects.requireNonNull(connection, "connection"));
            ENSURED.set(true);
        }
    }

    private static void ensureInternal(Connection connection) throws SQLException {
        // This project is often used against DBs restored from older dumps.
        // Missing columns currently crash the UI with: "Unknown column ... in field list".
        List<SchemaFix> fixes = List.of(
                // tournaments: added in migration Version20260226220425
                SchemaFix.addColumn("tournaments", "registration_mode",
                        "ALTER TABLE tournaments ADD COLUMN registration_mode VARCHAR(8) NOT NULL DEFAULT 'OPEN'"),
                SchemaFix.addColumn("tournaments", "photo_path",
                        "ALTER TABLE tournaments ADD COLUMN photo_path VARCHAR(255) DEFAULT NULL"),

                // categories: added in migration Version20260227160000
                SchemaFix.addColumn("categories", "slug",
                        "ALTER TABLE categories ADD COLUMN slug VARCHAR(191) DEFAULT NULL"),

                // users: added in migration Version20260226220425 / Version20260227130000
                SchemaFix.addColumn("users", "reset_password_token_hash",
                        "ALTER TABLE users ADD COLUMN reset_password_token_hash VARCHAR(64) DEFAULT NULL"),
                SchemaFix.addColumn("users", "reset_password_expires_at",
                        "ALTER TABLE users ADD COLUMN reset_password_expires_at DATETIME DEFAULT NULL"),
                SchemaFix.addColumn("users", "two_factor_enabled",
                        "ALTER TABLE users ADD COLUMN two_factor_enabled TINYINT(1) NOT NULL DEFAULT 0"),
                SchemaFix.addColumn("users", "two_factor_secret",
                        "ALTER TABLE users ADD COLUMN two_factor_secret VARCHAR(64) DEFAULT NULL"),
                SchemaFix.addColumn("users", "two_factor_enabled_at",
                        "ALTER TABLE users ADD COLUMN two_factor_enabled_at DATETIME DEFAULT NULL"),

                // games: added in migration Version20260227160000
                SchemaFix.addColumn("games", "slug",
                        "ALTER TABLE games ADD COLUMN slug VARCHAR(191) DEFAULT NULL"),
                SchemaFix.addColumn("games", "status",
                        "ALTER TABLE games ADD COLUMN status VARCHAR(10) NOT NULL DEFAULT 'DRAFT'"),
                SchemaFix.addColumn("games", "popularity_score",
                        "ALTER TABLE games ADD COLUMN popularity_score INT UNSIGNED NOT NULL DEFAULT 0"),
                SchemaFix.addColumn("games", "views_count",
                        "ALTER TABLE games ADD COLUMN views_count INT UNSIGNED NOT NULL DEFAULT 0"),
                SchemaFix.addColumn("games", "favorites_count",
                        "ALTER TABLE games ADD COLUMN favorites_count INT UNSIGNED NOT NULL DEFAULT 0"),
                SchemaFix.addColumn("games", "cover_name",
                        "ALTER TABLE games ADD COLUMN cover_name VARCHAR(255) DEFAULT NULL"),
                SchemaFix.addColumn("games", "reviewed_at",
                        "ALTER TABLE games ADD COLUMN reviewed_at DATETIME DEFAULT NULL")
        );

        List<SchemaFix> missing = new ArrayList<>();
        for (SchemaFix fix : fixes) {
            if (!columnExists(connection, fix.table(), fix.column())) {
                missing.add(fix);
            }
        }
        if (missing.isEmpty()) {
            return;
        }

        for (SchemaFix fix : missing) {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(fix.ddl());
            }
        }

        List<String> stillMissing = new ArrayList<>();
        for (SchemaFix fix : missing) {
            if (!columnExists(connection, fix.table(), fix.column())) {
                stillMissing.add(fix.table() + "." + fix.column());
            }
        }
        if (!stillMissing.isEmpty()) {
            throw new SQLException("Schema DB incomplet (colonnes manquantes): " + String.join(", ", stillMissing)
                    + ". Appliquez les migrations SQL / donnez les droits ALTER TABLE a l'utilisateur DB.");
        }
    }

    private static boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();
        String catalog = connection.getCatalog();

        if (columnExists(meta, catalog, tableName, columnName)) {
            return true;
        }
        // Some setups expose metadata in different casing.
        return columnExists(meta, catalog, tableName.toUpperCase(), columnName.toUpperCase())
                || columnExists(meta, catalog, tableName.toLowerCase(), columnName.toLowerCase());
    }

    private static boolean columnExists(DatabaseMetaData meta, String catalog, String tableName, String columnName) throws SQLException {
        try (ResultSet rs = meta.getColumns(catalog, null, tableName, columnName)) {
            return rs.next();
        }
    }

    private record SchemaFix(String table, String column, String ddl) {
        static SchemaFix addColumn(String table, String column, String ddl) {
            return new SchemaFix(table, column, ddl);
        }
    }
}
