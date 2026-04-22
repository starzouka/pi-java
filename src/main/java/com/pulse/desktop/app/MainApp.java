package com.pulse.desktop.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/fxml/app-shell.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1500, 900);
        scene.getStylesheets().add(MainApp.class.getResource("/css/app.css").toExternalForm());

        stage.setTitle("PULSE Desktop - JavaFX");
        stage.setMinWidth(1280);
        stage.setMinHeight(780);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
