package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.connection.MyConnection;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Charger le fichier FXML principal
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Front/ListProducts.fxml"));
        Parent root = loader.load();

        // Créer la scène
        Scene scene = new Scene(root, 1200, 700);

        // Charger les CSS globales (design moderne)
        String styleSheet = getClass().getResource("/css/style_javafx.css").toExternalForm();
        scene.getStylesheets().add(styleSheet);

        // Configurer la fenêtre
        primaryStage.setTitle("🎮 PiDeb - E-Sports Shop");
        primaryStage.setScene(scene);
        primaryStage.setWidth(1200);
        primaryStage.setHeight(700);
        primaryStage.setResizable(true);
        primaryStage.show();

        System.out.println("✓ Application PiDeb démarrée avec succès!");
        System.out.println("✓ CSS appliquées: " + styleSheet);
    }

    @Override
    public void stop() throws Exception {
        // Fermer la connexion à la base de données
        MyConnection.getInstance().close();
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}