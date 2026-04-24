package com.pulse.desktop.model;

import java.util.List;

public record AdminStatsSnapshot(
        long totalGames,
        long totalCategories,
        long totalViews,
        long viewsToday,
        List<NamedCount> gamesPerCategory,
        List<NamedCount> topViewedGames,
        List<DailyActionCounts> activityLast14Days
) {
}

