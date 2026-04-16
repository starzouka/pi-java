package org.example.controller;

import javafx.scene.control.TableCell;

/**
 * Classe utilitaire pour les cellules d'action dans les tables
 * Permet de gérer les boutons d'action sans utiliser de cellFactory en FXML
 */
public class ActionCell<T> extends TableCell<T, Void> {

    public ActionCell() {
        super();
    }
}

