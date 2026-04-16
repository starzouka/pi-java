package com.pulse.desktop.ui;

import com.pulse.desktop.auth.SessionContext;
import com.pulse.desktop.auth.SessionUser;
import com.pulse.desktop.model.ProfileData;
import com.pulse.desktop.model.ProfileFilters;
import com.pulse.desktop.model.RouteDefinition;
import com.pulse.desktop.repo.ProfileRepository;
import com.pulse.desktop.service.Navigator;
import com.pulse.desktop.util.AlertUtils;
import com.pulse.desktop.util.ImageResolver;
import com.pulse.desktop.util.Validators;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ProfileController implements RouteAwarePage {
    private static final DateTimeFormatter POST_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter COMMENT_DATE = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private static final String TAB_POSTS = "posts";
    private static final String TAB_ABOUT = "about";
    private static final String TAB_FRIENDS = "friends";
    private static final String TAB_TEAMS = "teams";

    @FXML
    private ImageView avatarView;
    @FXML
    private Label displayNameLabel;
    @FXML
    private Label usernameRoleLabel;
    @FXML
    private Label postsStatLabel;
    @FXML
    private Label friendsStatLabel;
    @FXML
    private Label teamsStatLabel;
    @FXML
    private Label feedbackLabel;

    @FXML
    private Button tabPostsButton;
    @FXML
    private Button tabAboutButton;
    @FXML
    private Button tabFriendsButton;
    @FXML
    private Button tabTeamsButton;

    @FXML
    private VBox postsPane;
    @FXML
    private VBox aboutPane;
    @FXML
    private VBox friendsPane;
    @FXML
    private VBox teamsPane;

    @FXML
    private TextArea postComposerField;
    @FXML
    private ComboBox<String> postComposerVisibilityCombo;
    @FXML
    private TextField postImagesField;

    @FXML
    private TextField postsSearchField;
    @FXML
    private ComboBox<String> postsVisibilityCombo;
    @FXML
    private ComboBox<String> postsSortCombo;
    @FXML
    private VBox postsListBox;

    @FXML
    private VBox aboutListBox;

    @FXML
    private TextField friendsSearchField;
    @FXML
    private ComboBox<String> friendsSortCombo;
    @FXML
    private VBox friendsListBox;

    @FXML
    private TextField teamsSearchField;
    @FXML
    private TextField teamsRegionField;
    @FXML
    private ComboBox<String> teamsSortCombo;
    @FXML
    private VBox teamsListBox;

    private final ProfileRepository profileRepository = new ProfileRepository();
    private final List<Path> selectedPostImages = new ArrayList<>();
    private String activeTab = TAB_POSTS;

    @FXML
    public void initialize() {
        postsVisibilityCombo.setItems(FXCollections.observableArrayList("", "PUBLIC", "FRIENDS", "TEAM_ONLY"));
        postsVisibilityCombo.getSelectionModel().select("");

        postsSortCombo.setItems(FXCollections.observableArrayList("latest", "oldest", "liked", "commented"));
        postsSortCombo.getSelectionModel().select("latest");

        friendsSortCombo.setItems(FXCollections.observableArrayList("recent", "oldest", "name"));
        friendsSortCombo.getSelectionModel().select("recent");

        teamsSortCombo.setItems(FXCollections.observableArrayList("latest", "oldest", "name", "region"));
        teamsSortCombo.getSelectionModel().select("latest");

        postComposerVisibilityCombo.setItems(FXCollections.observableArrayList("PUBLIC", "FRIENDS", "TEAM_ONLY"));
        postComposerVisibilityCombo.getSelectionModel().select("PUBLIC");

        postImagesField.setEditable(false);
        updatePostImagesField();
    }

    @Override
    public void setRoute(RouteDefinition routeDefinition) {
        SessionUser user = SessionContext.getCurrentUser();
        if (user == null) {
            Navigator.goTo("front_login");
            return;
        }

        activeTab = TAB_POSTS;
        feedbackLabel.setText("Publications, details, amis et equipes.");
        postComposerField.clear();
        selectedPostImages.clear();
        updatePostImagesField();
        refreshProfile();
        switchTab(activeTab);
    }

    @FXML
    private void refreshProfile() {
        SessionUser user = SessionContext.getCurrentUser();
        if (user == null) {
            Navigator.goTo("front_login");
            return;
        }

        try {
            ProfileData data = profileRepository.loadOwnProfile(user.getUserId(), collectFilters());
            if (data == null) {
                feedbackLabel.setText("Profil introuvable.");
                return;
            }

            renderIdentity(data.identity(), data.postCount(), data.friendCount(), data.teamCount());
            renderPosts(data.posts());
            renderAbout(data.identity());
            renderFriends(data.friends());
            renderTeams(data.teams());
        } catch (SQLException ex) {
            AlertUtils.error("Profil", "Impossible de charger le profil.\n" + ex.getMessage());
        }
    }

    @FXML
    private void choosePostImages() {
        Window window = postComposerField.getScene() == null ? null : postComposerField.getScene().getWindow();
        if (window == null) {
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir des photos");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.webp", "*.gif")
        );
        List<File> files = chooser.showOpenMultipleDialog(window);
        if (files == null || files.isEmpty()) {
            return;
        }

        selectedPostImages.clear();
        for (File file : files) {
            if (file == null) {
                continue;
            }
            selectedPostImages.add(file.toPath());
            if (selectedPostImages.size() >= 8) {
                break;
            }
        }
        updatePostImagesField();
    }

    @FXML
    private void clearPostImages() {
        selectedPostImages.clear();
        updatePostImagesField();
    }

    @FXML
    private void publishPost() {
        SessionUser user = SessionContext.getCurrentUser();
        if (user == null) {
            Navigator.goTo("front_login");
            return;
        }

        String content = postComposerField.getText();
        if (Validators.isBlank(content) && selectedPostImages.isEmpty()) {
            feedbackLabel.setText("Le post doit contenir du texte ou au moins une image.");
            return;
        }

        try {
            boolean created = profileRepository.createPost(
                    user.getUserId(),
                    content,
                    postComposerVisibilityCombo.getValue(),
                    selectedPostImages
            );
            if (!created) {
                feedbackLabel.setText("Le post doit contenir du texte ou au moins une image valide.");
                return;
            }

            postComposerField.clear();
            selectedPostImages.clear();
            updatePostImagesField();
            feedbackLabel.setText("Post publie avec succes.");
            activeTab = TAB_POSTS;
            refreshProfile();
            switchTab(TAB_POSTS);
        } catch (SQLException ex) {
            AlertUtils.error("Profil", "Impossible de publier le post.\n" + ex.getMessage());
        }
    }

    @FXML
    private void applyPostsFilters() {
        activeTab = TAB_POSTS;
        refreshProfile();
        switchTab(TAB_POSTS);
    }

    @FXML
    private void applyFriendsFilters() {
        activeTab = TAB_FRIENDS;
        refreshProfile();
        switchTab(TAB_FRIENDS);
    }

    @FXML
    private void applyTeamsFilters() {
        activeTab = TAB_TEAMS;
        refreshProfile();
        switchTab(TAB_TEAMS);
    }

    @FXML
    private void openProfileEdit() {
        Navigator.goTo("front_profile_edit");
    }

    @FXML
    private void openPasswordChange() {
        Navigator.goTo("front_password_change");
    }

    @FXML
    private void openTabPosts() {
        activeTab = TAB_POSTS;
        switchTab(activeTab);
    }

    @FXML
    private void openTabAbout() {
        activeTab = TAB_ABOUT;
        switchTab(activeTab);
    }

    @FXML
    private void openTabFriends() {
        activeTab = TAB_FRIENDS;
        switchTab(activeTab);
    }

    @FXML
    private void openTabTeams() {
        activeTab = TAB_TEAMS;
        switchTab(activeTab);
    }

    private void renderIdentity(ProfileData.Identity identity, int postCount, int friendCount, int teamCount) {
        displayNameLabel.setText(identity.displayName());
        String role = identity.role() == null ? "PLAYER" : identity.role();
        String country = identity.country() == null || identity.country().isBlank() ? "" : " - " + identity.country();
        usernameRoleLabel.setText("@" + identity.username() + " - " + role + country);

        postsStatLabel.setText(Integer.toString(postCount));
        friendsStatLabel.setText(Integer.toString(friendCount));
        teamsStatLabel.setText(Integer.toString(teamCount));

        String imagePath = identity.profileImagePath();
        if (imagePath == null || imagePath.isBlank()) {
            imagePath = "https://picsum.photos/seed/pulse_profile_" + identity.userId() + "/200/200";
        }
        String external = ImageResolver.toExternalForm(imagePath);
        if (external == null) {
            external = imagePath;
        }
        avatarView.setImage(new Image(external, true));
    }

    private void renderPosts(List<ProfileData.PostItem> posts) {
        postsListBox.getChildren().clear();
        if (posts.isEmpty()) {
            postsListBox.getChildren().add(emptyState("Aucune publication pour le moment."));
            return;
        }

        for (ProfileData.PostItem post : posts) {
            VBox card = new VBox(8);
            card.getStyleClass().addAll("panel", "profilePost");

            HBox head = new HBox(8);
            Label visibility = new Label(post.visibility() == null ? "PUBLIC" : post.visibility());
            visibility.getStyleClass().addAll("badge", "badge-info");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label createdAt = new Label(post.createdAt() == null ? "-" : post.createdAt().format(POST_DATE));
            createdAt.getStyleClass().add("muted");
            head.getChildren().addAll(visibility, spacer, createdAt);
            card.getChildren().add(head);

            if (post.contentText() != null && !post.contentText().isBlank()) {
                Label content = new Label(post.contentText());
                content.setWrapText(true);
                card.getChildren().add(content);
            }

            if (post.imagePaths() != null && !post.imagePaths().isEmpty()) {
                VBox mediaBox = new VBox(6);
                for (String imagePath : post.imagePaths()) {
                    if (imagePath == null || imagePath.isBlank()) {
                        continue;
                    }
                    String external = ImageResolver.toExternalForm(imagePath);
                    if (external == null) {
                        external = imagePath;
                    }
                    ImageView imageView = new ImageView(new Image(external, true));
                    imageView.setPreserveRatio(true);
                    imageView.setFitWidth(520);
                    imageView.getStyleClass().add("post-image-preview");
                    mediaBox.getChildren().add(imageView);
                }
                if (!mediaBox.getChildren().isEmpty()) {
                    card.getChildren().add(mediaBox);
                }
            }

            HBox summary = new HBox(12);
            Label likesSummary = new Label(post.likesCount() + " J'aime");
            likesSummary.getStyleClass().add("muted");
            Label commentsSummary = new Label(post.commentsCount() + " commentaires");
            commentsSummary.getStyleClass().add("muted");
            summary.getChildren().addAll(likesSummary, commentsSummary);
            card.getChildren().add(summary);

            HBox actions = new HBox(8);
            Button likeButton = new Button(post.likedByViewer() ? "J'aime (retirer)" : "J'aime");
            likeButton.getStyleClass().add(post.likedByViewer() ? "btn-soft" : "btn-ghost");
            likeButton.setOnAction(event -> toggleLike(post.postId()));

            TextField commentField = new TextField();
            commentField.setPromptText("Ecrire un commentaire...");
            commentField.getStyleClass().add("input");
            HBox.setHgrow(commentField, Priority.ALWAYS);

            Button commentButton = new Button("Commenter");
            commentButton.getStyleClass().add("btn-primary");
            commentButton.setOnAction(event -> addComment(post.postId(), commentField.getText()));

            actions.getChildren().addAll(likeButton, commentField, commentButton);
            card.getChildren().add(actions);

            if (post.comments() != null && !post.comments().isEmpty()) {
                VBox commentsBox = new VBox(6);
                for (ProfileData.PostCommentItem comment : post.comments()) {
                    commentsBox.getChildren().add(listItem(
                            (comment.authorDisplayName() == null ? "Utilisateur" : comment.authorDisplayName())
                                    + ": "
                                    + (comment.contentText() == null ? "" : comment.contentText()),
                            comment.createdAt() == null ? "-" : comment.createdAt().format(COMMENT_DATE)
                    ));
                }
                card.getChildren().add(commentsBox);
            }

            postsListBox.getChildren().add(card);
        }
    }

    private void toggleLike(int postId) {
        SessionUser user = SessionContext.getCurrentUser();
        if (user == null) {
            Navigator.goTo("front_login");
            return;
        }

        try {
            boolean changed = profileRepository.togglePostLike(postId, user.getUserId());
            if (!changed) {
                feedbackLabel.setText("Post introuvable.");
                return;
            }
            activeTab = TAB_POSTS;
            refreshProfile();
            switchTab(TAB_POSTS);
        } catch (SQLException ex) {
            AlertUtils.error("Profil", "Impossible de mettre a jour le like.\n" + ex.getMessage());
        }
    }

    private void addComment(int postId, String commentText) {
        SessionUser user = SessionContext.getCurrentUser();
        if (user == null) {
            Navigator.goTo("front_login");
            return;
        }

        try {
            boolean added = profileRepository.addPostComment(postId, user.getUserId(), commentText);
            if (!added) {
                feedbackLabel.setText("Le commentaire est vide ou le post est introuvable.");
                return;
            }
            activeTab = TAB_POSTS;
            refreshProfile();
            switchTab(TAB_POSTS);
        } catch (SQLException ex) {
            AlertUtils.error("Profil", "Impossible d'ajouter le commentaire.\n" + ex.getMessage());
        }
    }

    private void renderAbout(ProfileData.Identity identity) {
        aboutListBox.getChildren().clear();
        aboutListBox.getChildren().addAll(
                listItem("Nom affichage", identity.displayName()),
                listItem("Username", identity.username()),
                listItem("Role", identity.role()),
                listItem("Email", identity.email()),
                listItem("Pays", identity.country()),
                listItem("Bio", identity.bio() == null || identity.bio().isBlank() ? "Aucune bio." : identity.bio()),
                listItem("Telephone", identity.phone()),
                listItem("Date naissance", identity.birthDate() == null ? "-" : identity.birthDate().toString()),
                listItem("Genre", identity.gender()),
                listItem("2FA", identity.twoFactorEnabled() ? "ACTIVE" : "INACTIVE")
        );
    }

    private void renderFriends(List<ProfileData.FriendItem> friends) {
        friendsListBox.getChildren().clear();
        if (friends.isEmpty()) {
            friendsListBox.getChildren().add(emptyState("Aucun ami affiche."));
            return;
        }
        for (ProfileData.FriendItem friend : friends) {
            friendsListBox.getChildren().add(listItem(
                    friend.displayName() + " (@" + friend.username() + ")",
                    friend.role()
            ));
        }
    }

    private void renderTeams(List<ProfileData.TeamItem> teams) {
        teamsListBox.getChildren().clear();
        if (teams.isEmpty()) {
            teamsListBox.getChildren().add(emptyState("Aucune equipe active."));
            return;
        }
        for (ProfileData.TeamItem team : teams) {
            teamsListBox.getChildren().add(listItem(
                    team.name(),
                    team.region() + " - " + (team.membershipRole() == null ? "MEMBER" : team.membershipRole())
            ));
        }
    }

    private void switchTab(String tab) {
        boolean postsActive = TAB_POSTS.equals(tab);
        boolean aboutActive = TAB_ABOUT.equals(tab);
        boolean friendsActive = TAB_FRIENDS.equals(tab);
        boolean teamsActive = TAB_TEAMS.equals(tab);

        setTabActive(tabPostsButton, postsActive);
        setTabActive(tabAboutButton, aboutActive);
        setTabActive(tabFriendsButton, friendsActive);
        setTabActive(tabTeamsButton, teamsActive);

        postsPane.setManaged(postsActive);
        postsPane.setVisible(postsActive);
        aboutPane.setManaged(aboutActive);
        aboutPane.setVisible(aboutActive);
        friendsPane.setManaged(friendsActive);
        friendsPane.setVisible(friendsActive);
        teamsPane.setManaged(teamsActive);
        teamsPane.setVisible(teamsActive);
    }

    private void updatePostImagesField() {
        if (selectedPostImages.isEmpty()) {
            postImagesField.setText("Aucune photo selectionnee.");
            return;
        }
        postImagesField.setText(selectedPostImages.size() + " photo(s) selectionnee(s)");
    }

    private static void setTabActive(Button button, boolean active) {
        button.getStyleClass().remove("tab--active");
        if (active) {
            button.getStyleClass().add("tab--active");
        }
    }

    private ProfileFilters collectFilters() {
        return new ProfileFilters(
                safe(postsSearchField.getText()),
                safe(postsVisibilityCombo.getValue()),
                safe(postsSortCombo.getValue()),
                safe(friendsSearchField.getText()),
                safe(friendsSortCombo.getValue()),
                safe(teamsSearchField.getText()),
                safe(teamsRegionField.getText()),
                safe(teamsSortCombo.getValue())
        );
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static HBox listItem(String leftText, String rightText) {
        HBox row = new HBox(10);
        row.getStyleClass().add("list-item");
        row.setAlignment(Pos.CENTER_LEFT);

        Label left = new Label((leftText == null || leftText.isBlank()) ? "-" : leftText);
        left.setWrapText(true);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label right = new Label((rightText == null || rightText.isBlank()) ? "-" : rightText);
        right.getStyleClass().add("list-item-meta");

        row.getChildren().addAll(left, spacer, right);
        return row;
    }

    private static Label emptyState(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("empty-state");
        label.setWrapText(true);
        return label;
    }
}
