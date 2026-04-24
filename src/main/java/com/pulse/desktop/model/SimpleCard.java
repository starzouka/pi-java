package com.pulse.desktop.model;

public record SimpleCard(
        String title,
        String subtitle,
        String badgeLeft,
        String badgeRight,
        String targetRoute,
        String imagePath,
        String cardVariant
) {
    public SimpleCard(String title, String subtitle, String badgeLeft, String badgeRight, String targetRoute) {
        this(title, subtitle, badgeLeft, badgeRight, targetRoute, null, "generic");
    }

    public SimpleCard(String title, String subtitle, String badgeLeft, String badgeRight, String targetRoute, String imagePath) {
        this(title, subtitle, badgeLeft, badgeRight, targetRoute, imagePath, "generic");
    }
}
