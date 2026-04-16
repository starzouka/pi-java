package org.example.connection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Singleton pour la gestion de la connexion à la base de données
 * Utilise le pattern Singleton thread-safe
 */
public class MyConnection {
    private static volatile MyConnection instance;
    private Connection connection;
    private static final Logger LOGGER = Logger.getLogger(MyConnection.class.getName());

    // Configuration XAMPP
    private static final String URL = "jdbc:mysql://localhost:3306/pulsedb?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC&autoReconnect=true";
    private static final String USER = "root";
    private static final String PASSWORD = "";
    private static final String DRIVER = "com.mysql.cj.jdbc.Driver";

    /**
     * Constructeur privé pour empêcher l'instantiation
     */
    private MyConnection() {
        try {
            initializeConnection();
        } catch (ClassNotFoundException | SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'initialisation de la connexion: " + e.getMessage());
        }
    }

    /**
     * Initialiser la connexion à la base de données
     */
    private void initializeConnection() throws ClassNotFoundException, SQLException {
        try {
            Class.forName(DRIVER);
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            LOGGER.log(Level.INFO, "✓ Connexion à pulsedb réussie (XAMPP MySQL)!");
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "✗ Driver MySQL non trouvé: " + e.getMessage());
            throw e;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "✗ Erreur de connexion à la base de données: " + e.getMessage());
            LOGGER.log(Level.INFO, "Vérifications recommandées:");
            LOGGER.log(Level.INFO, "1. XAMPP MySQL est démarré");
            LOGGER.log(Level.INFO, "2. La base 'pulsedb' existe");
            LOGGER.log(Level.INFO, "3. L'URL est correcte: " + URL);
            throw e;
        }
    }

    /**
     * Obtenir l'instance unique (Singleton thread-safe)
     */
    public static MyConnection getInstance() {
        if (instance == null) {
            synchronized (MyConnection.class) {
                if (instance == null) {
                    instance = new MyConnection();
                }
            }
        }
        return instance;
    }

    /**
     * Obtenir la connexion
     */
    public synchronized Connection getConnection() {
        try {
            // Vérifier et reconnecter si nécessaire
            if (connection == null || connection.isClosed()) {
                LOGGER.log(Level.WARNING, "Connexion perdue, tentative de reconnexion...");
                reconnect();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la vérification de la connexion: " + e.getMessage());
            try {
                reconnect();
            } catch (SQLException reconnectError) {
                LOGGER.log(Level.SEVERE, "Impossible de se reconnecter: " + reconnectError.getMessage());
            }
        }
        return connection;
    }

    /**
     * Vérifier si la connexion est active
     */
    public synchronized boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Erreur lors de la vérification de connexion: " + e.getMessage());
            return false;
        }
    }

    /**
     * Se reconnecter à la base de données
     */
    private synchronized void reconnect() throws SQLException {
        try {
            if (connection != null) {
                connection.close();
            }
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            LOGGER.log(Level.INFO, "✓ Reconnexion réussie!");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la reconnexion: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Fermer la connexion
     */
    public synchronized void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                LOGGER.log(Level.INFO, "✓ Connexion fermée.");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "✗ Erreur lors de la fermeture de la connexion: " + e.getMessage());
        }
    }

    /**
     * Obtenir les informations de la connexion (pour le débogage)
     */
    public synchronized String getConnectionInfo() {
        try {
            if (isConnected()) {
                return "Connecté - " + connection.getMetaData().getDatabaseProductName() +
                       " " + connection.getMetaData().getDatabaseProductVersion();
            } else {
                return "Déconnecté";
            }
        } catch (SQLException e) {
            return "Erreur - " + e.getMessage();
        }
    }
}

