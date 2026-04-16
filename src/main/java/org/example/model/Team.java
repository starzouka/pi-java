package org.example.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Represents a team/organization that sells products.
 * Maps to `teams` table in the database.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Team {
    private int teamId;              // team_id PK
    private String name;             // varchar(100)
    private String description;      // longtext
    private String region;           // varchar(80)
    private LocalDateTime createdAt; // datetime
    private LocalDateTime updatedAt; // datetime
    private Integer logoImageId;     // FK → images.image_id
    private int captainUserId;       // FK → users.user_id

    // Convenience constructor for basic team info
    public Team(String name, int captainUserId) {
        this.name = name;
        this.captainUserId = captainUserId;
    }

    // Convenience constructor with description
    public Team(String name, String description, int captainUserId) {
        this.name = name;
        this.description = description;
        this.captainUserId = captainUserId;
    }

    // Convenience constructor with all fields
    public Team(String name, String description, String region, int captainUserId) {
        this.name = name;
        this.description = description;
        this.region = region;
        this.captainUserId = captainUserId;
    }

    @Override
    public String toString() {
        return "Team{" +
                "teamId=" + teamId +
                ", name='" + name + '\'' +
                ", region='" + region + '\'' +
                ", captainUserId=" + captainUserId +
                '}';
    }
}

