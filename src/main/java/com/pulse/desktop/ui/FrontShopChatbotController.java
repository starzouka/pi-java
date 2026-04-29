package com.pulse.desktop.ui;

import com.pulse.desktop.model.ChatMessage;
import com.pulse.desktop.model.RouteDefinition;
import com.pulse.desktop.service.Navigator;
import com.pulse.desktop.service.ShopChatbotService;
import com.pulse.desktop.util.AlertUtils;
import com.pulse.desktop.util.StringUtils;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class FrontShopChatbotController implements RouteAwarePage {
    @FXML
    private Label routeNameLabel;
    @FXML
    private Label routeIdLabel;
    @FXML
    private Label routeDescriptionLabel;
    @FXML
    private VBox messagesBox;
    @FXML
    private ScrollPane messagesScroll;
    @FXML
    private TextArea messageField;
    @FXML
    private Label statusLabel;
    @FXML
    private Button sendButton;

    private final ShopChatbotService chatbotService = new ShopChatbotService();
    private final List<ChatMessage> history = new ArrayList<>();
    private boolean initialized;

    @FXML
    public void initialize() {
        if (statusLabel != null) {
            statusLabel.setText(chatbotService.isConfigured()
                    ? "Assistant boutique prêt."
                    : "Configurez PULSE_OPENROUTER_API_KEY pour activer l'assistant.");
        }
    }

    @Override
    public void setRoute(RouteDefinition routeDefinition) {
        if (routeNameLabel != null) {
            routeNameLabel.setText(routeDefinition.title());
        }
        if (routeIdLabel != null) {
            routeIdLabel.setText("Route Symfony: " + routeDefinition.id());
        }
        if (routeDescriptionLabel != null) {
            routeDescriptionLabel.setText("Assistant limité à la boutique PULSE: produits, stock, panier et commandes.");
        }
        if (!initialized) {
            initialized = true;
            addAssistantMessage("Bonjour, je suis l'assistant boutique PULSE. Posez-moi une question sur les produits, le panier ou les commandes.");
            Platform.runLater(() -> {
                if (messagesScroll != null) {
                    messagesScroll.layout();
                    messagesScroll.setVvalue(1.0);
                }
            });
        }
    }

    @FXML
    private void sendMessage() {
        String prompt = StringUtils.safe(messageField.getText());
        if (prompt.isBlank()) {
            return;
        }

        if (!chatbotService.isConfigured()) {
            AlertUtils.warning("Assistant boutique", "Configurez d'abord OpenRouter pour activer le chatbot.");
            return;
        }

        List<ChatMessage> snapshot = new ArrayList<>(history);
        messageField.clear();
        addUserMessage(prompt);
        setBusy(true, "Analyse de votre demande...");
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return chatbotService.reply(snapshot, prompt);
            }
        };

        task.setOnSucceeded(event -> {
            addAssistantMessage(task.getValue());
            setBusy(false, "Réponse reçue.");
        });
        task.setOnFailed(event -> {
            Throwable error = task.getException();
            String fallback = "Désolé, je n'ai pas pu joindre OpenRouter pour le moment.";
            if (error != null && error.getMessage() != null && !error.getMessage().isBlank()) {
                fallback += "\n" + error.getMessage();
            }
            addAssistantMessage(fallback);
            setBusy(false, "Réponse indisponible.");
        });

        Thread worker = new Thread(task, "shop-chatbot-openrouter");
        worker.setDaemon(true);
        worker.start();
    }

    @FXML
    private void clearConversation() {
        history.clear();
        messagesBox.getChildren().clear();
        addAssistantMessage("Conversation réinitialisée. Je peux vous aider sur la boutique.");
        setStatus("Conversation nettoyée.");
    }

    @FXML
    private void backToShop() {
        Navigator.goTo("front_shop");
    }

    @FXML
    private void askProductAdvice() {
        usePreset("Quel produit me conseillez-vous selon mon budget ?");
    }

    @FXML
    private void askStockHelp() {
        usePreset("Aidez-moi à comprendre les stocks et la disponibilité des produits.");
    }

    @FXML
    private void askOrderHelp() {
        usePreset("Comment suivre ma commande ou mon panier ?");
    }

    private void usePreset(String text) {
        messageField.setText(text);
        messageField.positionCaret(text.length());
        sendMessage();
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
        while (history.size() > 20) {
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
        String role = StringUtils.safe(message.role()).toLowerCase();
        boolean user = "user".equals(role);
        boolean assistant = "assistant".equals(role);

        Label roleLabel = new Label(user ? "Vous" : assistant ? "Assistant boutique" : "Système");
        roleLabel.getStyleClass().add("chat-bubble__role");

        Label content = new Label(StringUtils.safe(message.content()));
        content.setWrapText(true);
        content.getStyleClass().add("chat-bubble__content");

        VBox bubble = new VBox(4, roleLabel, content);
        bubble.getStyleClass().add("chat-bubble");
        bubble.getStyleClass().add(user ? "chat-bubble--user" : "chat-bubble--assistant");
        bubble.setMaxWidth(580);

        HBox row = new HBox(8);
        row.setAlignment(user ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        row.setPrefWidth(Double.MAX_VALUE);
        row.getChildren().add(bubble);

        VBox wrapper = new VBox(row);
        wrapper.getStyleClass().add("chat-line");
        return wrapper;
    }

    private void setBusy(boolean busy, String status) {
        if (sendButton != null) {
            sendButton.setDisable(busy);
        }
        if (messageField != null) {
            messageField.setDisable(busy);
        }
        setStatus(status);
    }

    private void setStatus(String status) {
        if (statusLabel != null) {
            statusLabel.setText(status);
        }
    }
}

