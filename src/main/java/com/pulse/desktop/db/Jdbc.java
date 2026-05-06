package com.pulse.desktop.db;

import com.pulse.desktop.config.AppConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Jdbc {
    private Jdbc() {
    }

    public static Connection open() throws SQLException {
        Connection connection = DriverManager.getConnection(AppConfig.jdbcUrl(), AppConfig.dbUser(), AppConfig.dbPassword());
        SchemaBootstrap.ensure(connection);
        return connection;
    }
}
