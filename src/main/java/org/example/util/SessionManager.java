package org.example.util;

import org.example.model.User;

/**
 * Gestionnaire de session utilisateur
 * Gère l'utilisateur actuellement connecté
 */
public class SessionManager {
    private static SessionManager instance;
    private User currentUser;

    private SessionManager() {
        // Private constructor pour singleton
    }

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    /**
     * Définir l'utilisateur actuellement connecté
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    /**
     * Obtenir l'utilisateur actuellement connecté
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Obtenir l'ID de l'utilisateur actuellement connecté
     */
    public int getCurrentUserId() {
        if (currentUser != null) {
            return currentUser.getUserId();
        }
        return 1; // Valeur par défaut pour les tests
    }

    /**
     * Vérifier si un utilisateur est connecté
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * Déconnecter l'utilisateur
     */
    public void logout() {
        this.currentUser = null;
    }

    /**
     * Réinitialiser la session
     */
    public void clear() {
        this.currentUser = null;
    }
}

