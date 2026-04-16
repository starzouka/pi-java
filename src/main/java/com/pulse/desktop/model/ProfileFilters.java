package com.pulse.desktop.model;

public record ProfileFilters(
        String postsQuery,
        String postsVisibility,
        String postsSort,
        String friendsQuery,
        String friendsSort,
        String teamsQuery,
        String teamsRegion,
        String teamsSort
) {
    public static ProfileFilters defaults() {
        return new ProfileFilters("", "", "latest", "", "recent", "", "", "latest");
    }
}
