package com.pulse.desktop.model;

import java.time.LocalDate;

public record DailyCount(LocalDate day, long count) {
}

