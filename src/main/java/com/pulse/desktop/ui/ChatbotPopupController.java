package com.pulse.desktop.ui;

import com.pulse.desktop.model.ChatMessage;
import com.pulse.desktop.service.ShopChatbotService;
import com.pulse.desktop.util.AlertUtils;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ChatbotPopupController {
    @FXML
    private ScrollPane messagesScroll;
    @FXML
    private VBox messagesBox;
    @FXML
    private TextArea messageField;
    @FXML
    private Label statusLabel;
    @FXML
    private Button sendButton;

    private final ShopChatbotService chatbotService = new ShopChatbotService();
    private final List<ChatMessage> history = new ArrayList<>();
    private Stage stage;

    @FXML
    public void initialize() {
        if (statusLabel != null) {
            statusLabel.setText(chatbotService.isConfigured() ? "Prêt" : "Non configuré");
        }
        addAssistantMessage("Bonjour! 👋 Je suis l'assistant boutique PULSE. Posez-moi une question sur les produits, le stock ou vos commandes.");
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    private void sendMessage() {
        String prompt = safe(messageField.getText());
        if (prompt.isBlank()) {
            return;
        }

        if (!chatbotService.isConfigured()) {
            AlertUtils.warning("Assistant", "OpenRouter non configuré.");
            return;
        }

        List<ChatMessage> snapshot = new ArrayList<>(history);
        messageField.clear();
        addUserMessage(prompt);
        setBusy(true);

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return chatbotService.reply(snapshot, prompt);
            }
        };

        task.setOnSucceeded(event -> {
            addAssistantMessage(task.getValue());
            setBusy(false);
        });
        task.setOnFailed(event -> {
            String error = task.getException() != null ? task.getException().getMessage() : "Erreur inconnue";
            addAssistantMessage("Désolé, je n'ai pas pu traiter votre demande.\n" + error);
            setBusy(false);
        });

        Thread worker = new Thread(task, "chatbot-popup-worker");
        worker.setDaemon(true);
        worker.start();
    }

    @FXML
    private void closeWindow() {
        if (stage != null) {
            stage.close();
        }
    }

    private void addUserMessage(String text) {
        history.add(new ChatMessage("user", text));
        trimHistory();
        renderMessages();
    }

    private void addAssistantMessage(String text) {
        history.add(new ChatMessage("assistant", text));
        trimHistory();
        renderMessages();
    }

    private void trimHistory() {
        while (history.size() > 16) {
            history.remove(0);
        }
    }

    private void renderMessages() {
        if (messagesBox == null) {
            return;
        }
        messagesBox.getChildren().clear();
        for (ChatMessage message : history) {
            messagesBox.getChildren().add(buildBubble(message));
        }
        Platform.runLater(() -> {
            if (messagesScroll != null) {
                messagesScroll.layout();
                messagesScroll.setVvalue(1.0);
            }
        });
    }

    private VBox buildBubble(ChatMessage message) {
        String role = safe(message.role()).toLowerCase();
        boolean user = "user".equals(role);

        Label content = new Label(safe(message.content()));
        content.setWrapText(true);
        content.getStyleClass().add("chatbot-popup__bubble-text");

        VBox bubble = new VBox(content);
        bubble.getStyleClass().add("chatbot-popup__bubble");
        bubble.getStyleClass().add(user ? "chatbot-popup__bubble--user" : "chatbot-popup__bubble--assistant");
        bubble.setMaxWidth(340);

        HBox row = new HBox(bubble);
        row.setAlignment(user ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        row.setPrefWidth(Double.MAX_VALUE);

        VBox wrapper = new VBox(row);
        wrapper.getStyleClass().add("chatbot-popup__line");
        return wrapper;
    }

    private void setBusy(boolean busy) {
        if (sendButton != null) {
            sendButton.setDisable(busy);
        }
        if (messageField != null) {
            messageField.setDisable(busy);
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public static Stage create() {
        try {
            FXMLLoader loader = new FXMLLoader(ChatbotPopupController.class.getResource("/fxml/pages/routes/chatbot-popup-view.fxml"));
            Parent root = loader.load();
            ChatbotPopupController controller = loader.getController();

            Stage stage = new Stage(StageStyle.TRANSPARENT);
            stage.setTitle("Assistant Boutique PULSE");
            stage.setAlwaysOnTop(false);
            Scene scene = new Scene(root);
            scene.getStylesheets().add(ChatbotPopupController.class.getResource("/css/app.css").toExternalForm());
            stage.setScene(scene);
            controller.setStage(stage);

            stage.setWidth(420);
            stage.setHeight(600);

            return stage;
        } catch (IOException ex) {
            AlertUtils.error("Chatbot", "Impossible d'ouvrir l'assistant.\n" + ex.getMessage());
            return null;
        }
    }
}

