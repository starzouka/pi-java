package com.pulse.desktop.model;

public record DashboardStats(
        long users,
        long games,
        long teams,
        long tournaments,
        long products,
        long orders
) {
}
