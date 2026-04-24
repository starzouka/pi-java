package com.pulse.desktop.ui;

import com.pulse.desktop.model.AdminStatsSnapshot;
import com.pulse.desktop.model.DailyActionCounts;
import com.pulse.desktop.model.DailyCount;
import com.pulse.desktop.model.GameModel;
import com.pulse.desktop.model.NamedCount;
import com.pulse.desktop.model.RouteDefinition;
import com.pulse.desktop.repo.AdminStatsRepository;
import com.pulse.desktop.repo.GameRepository;
import com.pulse.desktop.util.AlertUtils;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminStatsController implements RouteAwarePage {
    private static final DateTimeFormatter UPDATED_AT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @FXML private ScrollPane rootScroll;

    @FXML private Label totalGamesLabel;
    @FXML private Label totalCategoriesLabel;
    @FXML private Label totalViewsLabel;
    @FXML private Label viewsTodayLabel;
    @FXML private Label lastUpdatedLabel;
    @FXML private Label activityDetailLabel;
    @FXML private Label gameViewsDetailLabel;

    @FXML private StackedBarChart<String, Number> activityChart;
    @FXML private BarChart<String, Number> topViewsChart;
    @FXML private PieChart gamesPerCategoryChart;
    @FXML private ComboBox<GameModel> gameViewsGameBox;
    @FXML private LineChart<String, Number> gameViewsChart;

    private final AdminStatsRepository repository = new AdminStatsRepository();
    private final GameRepository gameRepo = new GameRepository();
    private Timeline autoRefresh;
    private final Map<String, DailyActionCounts> activityByDay = new HashMap<>();
    private final ObservableList<GameModel> gameOptions = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        if (activityChart != null) {
            activityChart.setAnimated(false);
            activityChart.setLegendVisible(true);
            activityChart.setCategoryGap(8.0);
        }
        if (topViewsChart != null) {
            topViewsChart.setAnimated(false);
            topViewsChart.setLegendVisible(false);
        }
        if (gamesPerCategoryChart != null) {
            gamesPerCategoryChart.setLegendVisible(true);
            gamesPerCategoryChart.setLabelsVisible(true);
        }
        if (gameViewsChart != null) {
            gameViewsChart.setAnimated(false);
            gameViewsChart.setLegendVisible(false);
            gameViewsChart.setCreateSymbols(true);
        }

        setupGameViewsBox();
        refresh(false);

        autoRefresh = new Timeline(new KeyFrame(Duration.seconds(5), event -> refresh(false)));
        autoRefresh.setCycleCount(Animation.INDEFINITE);

        if (rootScroll != null) {
            rootScroll.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (autoRefresh == null) {
                    return;
                }
                if (newScene == null) {
                    autoRefresh.stop();
                } else if (autoRefresh.getStatus() != Animation.Status.RUNNING) {
                    autoRefresh.play();
                }
            });
            if (rootScroll.getScene() != null) {
                autoRefresh.play();
            }
        } else {
            autoRefresh.play();
        }
    }

    @FXML
    private void refreshManual() {
        refresh(true);
    }

    private void refresh(boolean showPopupOnError) {
        try {
            AdminStatsSnapshot snapshot = repository.loadSnapshot();
            render(snapshot);
            refreshGameOptions();
            refreshGameViews(false);
            if (lastUpdatedLabel != null) {
                lastUpdatedLabel.setText("Mis a jour: " + LocalDateTime.now().format(UPDATED_AT));
            }
        } catch (Exception ex) {
            if (lastUpdatedLabel != null) {
                lastUpdatedLabel.setText("Erreur stats: " + (ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()));
            }
            if (showPopupOnError) {
                AlertUtils.error("Stats", "Impossible de charger les statistiques.\n" + ex.getMessage());
            }
        }
    }

    private void render(AdminStatsSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        if (totalGamesLabel != null) totalGamesLabel.setText(Long.toString(snapshot.totalGames()));
        if (totalCategoriesLabel != null) totalCategoriesLabel.setText(Long.toString(snapshot.totalCategories()));
        if (totalViewsLabel != null) totalViewsLabel.setText(Long.toString(snapshot.totalViews()));
        if (viewsTodayLabel != null) viewsTodayLabel.setText(Long.toString(snapshot.viewsToday()));

        renderActivity(snapshot.activityLast14Days());
        renderTopViews(snapshot.topViewedGames());
        renderGamesPerCategory(snapshot.gamesPerCategory());
    }

    private void renderActivity(List<DailyActionCounts> rows) {
        if (activityChart == null) {
            return;
        }
        activityChart.getData().clear();
        activityByDay.clear();

        XYChart.Series<String, Number> views = new XYChart.Series<>();
        views.setName("Vues");
        XYChart.Series<String, Number> created = new XYChart.Series<>();
        created.setName("Creations");
        XYChart.Series<String, Number> updated = new XYChart.Series<>();
        updated.setName("Modifs");
        XYChart.Series<String, Number> deleted = new XYChart.Series<>();
        deleted.setName("Suppressions");

        if (rows != null) {
            for (DailyActionCounts row : rows) {
                String day = row.day() == null ? "" : row.day().toString();
                activityByDay.put(day, row);
                views.getData().add(new XYChart.Data<>(day, row.views()));
                created.getData().add(new XYChart.Data<>(day, row.created()));
                updated.getData().add(new XYChart.Data<>(day, row.updated()));
                deleted.getData().add(new XYChart.Data<>(day, row.deleted()));
            }
        }

        activityChart.getData().addAll(views, created, updated, deleted);
        installActivityTooltips(activityChart);

        if (activityDetailLabel != null && rows != null && !rows.isEmpty()) {
            DailyActionCounts last = rows.get(rows.size() - 1);
            updateActivityDetail(last);
        }
    }

    private void renderTopViews(List<NamedCount> rows) {
        if (topViewsChart == null) {
            return;
        }
        topViewsChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        if (rows != null) {
            for (NamedCount row : rows) {
                String name = row.name() == null ? "" : row.name().trim();
                if (name.length() > 18) {
                    name = name.substring(0, 18) + "…";
                }
                XYChart.Data<String, Number> data = new XYChart.Data<>(name, row.count());
                data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                    if (newNode == null) {
                        return;
                    }
                    Tooltip.install(newNode, new Tooltip(row.name() + ": " + row.count() + " vue(s)"));
                });
                series.getData().add(data);
            }
        }
        topViewsChart.getData().add(series);
    }

    private void renderGamesPerCategory(List<NamedCount> rows) {
        if (gamesPerCategoryChart == null) {
            return;
        }
        gamesPerCategoryChart.getData().clear();
        if (rows == null) {
            return;
        }
        for (NamedCount row : rows) {
            String name = row.name() == null ? "-" : row.name().trim();
            PieChart.Data data = new PieChart.Data(name, row.count());
            data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode == null) {
                    return;
                }
                Tooltip.install(newNode, new Tooltip(name + ": " + row.count() + " jeu(x)"));
            });
            gamesPerCategoryChart.getData().add(data);
        }
    }

    private void setupGameViewsBox() {
        if (gameViewsGameBox == null) {
            return;
        }
        gameViewsGameBox.setItems(gameOptions);
        gameViewsGameBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(GameModel object) {
                return object == null ? "" : safe(object.getName());
            }

            @Override
            public GameModel fromString(String string) {
                return null;
            }
        });
        gameViewsGameBox.setOnAction(event -> refreshGameViews(false));
        refreshGameOptions();
    }

    private void refreshGameOptions() {
        if (gameViewsGameBox == null) {
            return;
        }
        Integer keepId = null;
        GameModel selected = gameViewsGameBox.getSelectionModel().getSelectedItem();
        if (selected != null) {
            keepId = selected.getGameId();
        }
        try {
            List<GameModel> games = gameRepo.findAll();
            gameOptions.setAll(games);

            if (keepId != null) {
                for (GameModel g : gameOptions) {
                    if (g.getGameId() != null && g.getGameId().equals(keepId)) {
                        gameViewsGameBox.getSelectionModel().select(g);
                        return;
                    }
                }
            }
            if (!gameOptions.isEmpty() && gameViewsGameBox.getSelectionModel().isEmpty()) {
                gameViewsGameBox.getSelectionModel().select(0);
            }
        } catch (Exception ex) {
            if (gameViewsDetailLabel != null) {
                gameViewsDetailLabel.setText("Impossible de charger la liste des jeux: " + ex.getMessage());
            }
        }
    }

    private void refreshGameViews(boolean showPopupOnError) {
        if (gameViewsChart == null || gameViewsGameBox == null) {
            return;
        }
        GameModel selected = gameViewsGameBox.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getGameId() == null) {
            gameViewsChart.getData().clear();
            return;
        }
        try {
            List<DailyCount> rows = repository.loadGameViewsLastDays(selected.getGameId(), 30);
            renderGameViews(selected, rows);
        } catch (Exception ex) {
            if (showPopupOnError) {
                AlertUtils.error("Stats", "Impossible de charger les vues du jeu.\n" + ex.getMessage());
            }
        }
    }

    private void renderGameViews(GameModel game, List<DailyCount> rows) {
        if (gameViewsChart == null) {
            return;
        }
        gameViewsChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        long total = 0L;
        long today = 0L;
        String todayKey = java.time.LocalDate.now().toString();

        if (rows != null) {
            for (DailyCount row : rows) {
                String day = row.day() == null ? "" : row.day().toString();
                long count = row.count();
                total += count;
                if (todayKey.equals(day)) {
                    today = count;
                }
                XYChart.Data<String, Number> data = new XYChart.Data<>(day, count);
                data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                    if (newNode == null) {
                        return;
                    }
                    Tooltip.install(newNode, new Tooltip(day + "\nVues: " + count));
                });
                series.getData().add(data);
            }
        }

        gameViewsChart.getData().add(series);
        if (gameViewsDetailLabel != null) {
            gameViewsDetailLabel.setText("Jeu: " + safe(game.getName()) + " | Vues (30 jours): " + total + " | Aujourd'hui: " + today);
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private void installActivityTooltips(StackedBarChart<String, Number> chart) {
        for (XYChart.Series<String, Number> series : chart.getData()) {
            String seriesName = series.getName();
            for (XYChart.Data<String, Number> data : series.getData()) {
                data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                    if (newNode == null) {
                        return;
                    }
                    String day = data.getXValue();
                    long v = data.getYValue() == null ? 0L : data.getYValue().longValue();
                    DailyActionCounts counts = activityByDay.get(day);
                    long total = counts == null ? v : (counts.views() + counts.created() + counts.updated() + counts.deleted());
                    Tooltip.install(newNode, new Tooltip(day + "\n" + seriesName + ": " + v + "\nTotal: " + total));
                    newNode.setOnMouseClicked(event -> {
                        if (counts != null) {
                            updateActivityDetail(counts);
                        }
                    });
                });
            }
        }
    }

    private void updateActivityDetail(DailyActionCounts counts) {
        if (activityDetailLabel == null || counts == null) {
            return;
        }
        String day = counts.day() == null ? "-" : counts.day().toString();
        activityDetailLabel.setText(
                "Jour " + day
                        + " | Vues: " + counts.views()
                        + " | Creations: " + counts.created()
                        + " | Modifs: " + counts.updated()
                        + " | Suppressions: " + counts.deleted()
        );
    }

    @Override
    public void setRoute(RouteDefinition routeDefinition) {
        // No route-specific state needed.
    }
}
