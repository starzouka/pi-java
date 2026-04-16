package org.example.util;

import javafx.scene.Scene;

/**
 * Utility class to apply global CSS styling to all scenes
 */
public class SceneUtils {
    private static final String STYLESHEET = "/css/style_javafx.css";

    /**
     * Apply global CSS stylesheet to a scene
     * @param scene The JavaFX scene to style
     */
    public static void applyStylesheet(Scene scene) {
        String styleSheet = SceneUtils.class.getResource(STYLESHEET).toExternalForm();
        if (!scene.getStylesheets().contains(styleSheet)) {
            scene.getStylesheets().add(styleSheet);
        }
    }

    /**
     * Get the stylesheet URL
     * @return The stylesheet external form URL
     */
    public static String getStylesheetURL() {
        return SceneUtils.class.getResource(STYLESHEET).toExternalForm();
    }

    /**
     * Apply stylesheet and set standard window title
     * @param scene The JavaFX scene to style
     * @param title The window title
     */
    public static void applyStandardStyling(Scene scene, String title) {
        applyStylesheet(scene);
    }
}

