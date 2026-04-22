package com.pulse.desktop.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ProfileData(
        Identity identity,
        List<PostItem> posts,
        List<FriendItem> friends,
        List<TeamItem> teams,
        int postCount,
        int friendCount,
        int teamCount
) {
    public record Identity(
            int userId,
            String username,
            String email,
            String displayName,
            String role,
            String bio,
            String phone,
            String country,
            LocalDate birthDate,
            String gender,
            boolean active,
            boolean twoFactorEnabled,
            LocalDateTime twoFactorEnabledAt,
            String profileImagePath
    ) {
    }

    public record PostItem(
            int postId,
            String contentText,
            String visibility,
            LocalDateTime createdAt,
            int likesCount,
            int commentsCount,
            boolean likedByViewer,
            List<String> imagePaths,
            List<PostCommentItem> comments
    ) {
    }

    public record PostCommentItem(
            int commentId,
            String authorDisplayName,
            String contentText,
            LocalDateTime createdAt
    ) {
    }

    public record FriendItem(
            int userId,
            String username,
            String displayName,
            String role
    ) {
    }

    public record TeamItem(
            int teamId,
            String name,
            String region,
            String membershipRole
    ) {
    }
}
