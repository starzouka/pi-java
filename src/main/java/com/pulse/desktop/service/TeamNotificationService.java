package com.pulse.desktop.service;

import com.pulse.desktop.db.Jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class TeamNotificationService {
    private final MailService mailService = new MailService();
    private final DiscordWebhookService discordWebhookService = new DiscordWebhookService();

    public boolean isConfigured() {
        return mailService.isConfigured() || discordWebhookService.isConfigured();
    }

    public void notifyJoinRequestSubmitted(int teamId, int applicantUserId, String note) {
        if (!isConfigured() || teamId <= 0 || applicantUserId <= 0) {
            return;
        }
        dispatchAsync(() -> {
            try (Connection connection = Jdbc.open()) {
                TeamContact team = loadTeamContact(connection, teamId);
                UserContact applicant = loadUserContact(connection, applicantUserId);
                if (team == null || applicant == null) {
                    return;
                }

                if (mailService.isConfigured() && !isBlank(team.captainEmail())) {
                    mailService.sendTeamJoinRequestSubmitted(
                            team.captainEmail(),
                            team.captainName(),
                            team.teamName(),
                            applicant.displayName(),
                            note
                    );
                }

                if (mailService.isConfigured() && !isBlank(applicant.email())) {
                    mailService.sendTeamJoinRequestApplicantConfirmation(
                            applicant.email(),
                            applicant.displayName(),
                            team.teamName(),
                            team.captainName(),
                            note
                    );
                }

                if (discordWebhookService.isConfigured()) {
                    discordWebhookService.sendTeamEvent(
                            "Nouvelle demande equipe",
                            applicant.displayName()
                                    + " a demande de rejoindre "
                                    + team.teamName()
                                    + " (capitaine: "
                                    + team.captainName()
                                    + "). Message: "
                                    + safeText(note, "Aucun message.")
                    );
                }
            } catch (Exception ignored) {
                // Les notifications ne doivent pas bloquer le flux principal.
            }
        });
    }

    public void notifyInviteSent(int teamId, int captainUserId, int invitedUserId, String message) {
        if (!isConfigured() || teamId <= 0 || captainUserId <= 0 || invitedUserId <= 0) {
            return;
        }
        dispatchAsync(() -> {
            try (Connection connection = Jdbc.open()) {
                TeamContact team = loadTeamContact(connection, teamId);
                UserContact captain = loadUserContact(connection, captainUserId);
                UserContact invited = loadUserContact(connection, invitedUserId);
                if (team == null || captain == null || invited == null) {
                    return;
                }

                if (mailService.isConfigured() && !isBlank(invited.email())) {
                    mailService.sendTeamInviteNotification(
                            invited.email(),
                            invited.displayName(),
                            team.teamName(),
                            captain.displayName(),
                            message
                    );
                }

                if (discordWebhookService.isConfigured()) {
                    discordWebhookService.sendTeamEvent(
                            "Invitation equipe envoyee",
                            captain.displayName()
                                    + " a invite "
                                    + invited.displayName()
                                    + " dans "
                                    + team.teamName()
                                    + ". Message: "
                                    + safeText(message, "Invitation envoyee depuis PULSE.")
                    );
                }
            } catch (Exception ignored) {
                // Les notifications ne doivent pas bloquer le flux principal.
            }
        });
    }

    public void notifyMemberJoinedAfterAcceptedRequest(int teamId, int memberUserId) {
        if (!isConfigured() || teamId <= 0 || memberUserId <= 0) {
            return;
        }
        dispatchAsync(() -> {
            try (Connection connection = Jdbc.open()) {
                TeamContact team = loadTeamContact(connection, teamId);
                UserContact member = loadUserContact(connection, memberUserId);
                if (team == null || member == null) {
                    return;
                }

                if (mailService.isConfigured() && !isBlank(member.email())) {
                    mailService.sendTeamMemberJoinedNotification(
                            member.email(),
                            member.displayName(),
                            team.teamName(),
                            member.displayName()
                    );
                }
                if (mailService.isConfigured() && !isBlank(team.captainEmail()) && !equalsIgnoreCase(team.captainEmail(), member.email())) {
                    mailService.sendTeamMemberJoinedNotification(
                            team.captainEmail(),
                            team.captainName(),
                            team.teamName(),
                            member.displayName()
                    );
                }

                if (discordWebhookService.isConfigured()) {
                    discordWebhookService.sendTeamEvent(
                            "Nouveau membre",
                            member.displayName() + " a rejoint l'equipe " + team.teamName() + "."
                    );
                }
            } catch (Exception ignored) {
                // Les notifications ne doivent pas bloquer le flux principal.
            }
        });
    }

    public void notifyJoinRequestReviewed(int teamId, int applicantUserId, String decision) {
        if (!isConfigured() || teamId <= 0 || applicantUserId <= 0) {
            return;
        }

        String normalized = decision == null ? "" : decision.trim().toUpperCase(Locale.ROOT);
        if ("ACCEPTED".equals(normalized)) {
            // Deja couvert par notifyMemberJoinedAfterAcceptedRequest pour eviter le doublon.
            return;
        }
        if (!"REFUSED".equals(normalized) && !"CANCELLED".equals(normalized)) {
            return;
        }

        dispatchAsync(() -> {
            try (Connection connection = Jdbc.open()) {
                TeamContact team = loadTeamContact(connection, teamId);
                UserContact applicant = loadUserContact(connection, applicantUserId);
                if (team == null || applicant == null) {
                    return;
                }

                if (mailService.isConfigured() && !isBlank(applicant.email())) {
                    mailService.sendTeamJoinRequestDecision(
                            applicant.email(),
                            applicant.displayName(),
                            team.teamName(),
                            normalized
                    );
                }

                if (discordWebhookService.isConfigured()) {
                    discordWebhookService.sendTeamEvent(
                            "Decision demande equipe",
                            "Demande de " + applicant.displayName()
                                    + " pour " + team.teamName()
                                    + " -> " + decisionLabel(normalized) + "."
                    );
                }
            } catch (Exception ignored) {
                // Les notifications ne doivent pas bloquer le flux principal.
            }
        });
    }

    private TeamContact loadTeamContact(Connection connection, int teamId) throws Exception {
        String sql = """
                SELECT
                    t.team_id,
                    t.name AS team_name,
                    u.user_id AS captain_user_id,
                    COALESCE(NULLIF(u.display_name, ''), u.username) AS captain_name,
                    u.email AS captain_email
                FROM teams t
                INNER JOIN users u ON u.user_id = t.captain_user_id
                WHERE t.team_id = ?
                LIMIT 1
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, teamId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new TeamContact(
                        rs.getInt("team_id"),
                        rs.getString("team_name"),
                        rs.getInt("captain_user_id"),
                        rs.getString("captain_name"),
                        rs.getString("captain_email")
                );
            }
        }
    }

    private UserContact loadUserContact(Connection connection, int userId) throws Exception {
        String sql = """
                SELECT
                    u.user_id,
                    COALESCE(NULLIF(u.display_name, ''), u.username) AS display_name,
                    u.email
                FROM users u
                WHERE u.user_id = ?
                LIMIT 1
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new UserContact(
                        rs.getInt("user_id"),
                        rs.getString("display_name"),
                        rs.getString("email")
                );
            }
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isBlank();
    }

    private static boolean equalsIgnoreCase(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return left.trim().equalsIgnoreCase(right.trim());
    }

    private static String safeText(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.isBlank() ? fallback : normalized;
    }

    private static String decisionLabel(String normalizedDecision) {
        return switch (normalizedDecision) {
            case "REFUSED" -> "refusee";
            case "CANCELLED" -> "annulee";
            default -> normalizedDecision.toLowerCase(Locale.ROOT);
        };
    }

    private static void dispatchAsync(EmailAction action) {
        CompletableFuture.runAsync(() -> {
            try {
                action.run();
            } catch (Exception ignored) {
                // Les emails ne doivent pas bloquer le flux principal.
            }
        });
    }

    @FunctionalInterface
    private interface EmailAction {
        void run() throws Exception;
    }

    private record TeamContact(
            int teamId,
            String teamName,
            int captainUserId,
            String captainName,
            String captainEmail
    ) {
    }

    private record UserContact(
            int userId,
            String displayName,
            String email
    ) {
    }
}
