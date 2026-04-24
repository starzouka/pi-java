package com.pulse.desktop.model;

import java.time.LocalDate;

public record DailyActionCounts(
        LocalDate day,
        long views,
        long created,
        long updated,
        long deleted
) {
}

