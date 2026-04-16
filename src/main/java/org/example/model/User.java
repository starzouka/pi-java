package org.example.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a user in the system.
 * Maps to `users` table in the database.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private int userId;                    // user_id PK
    private String username;               // varchar(50) UNIQUE
    private String email;                  // varchar(190) UNIQUE
    private String passwordHash;           // varchar(255)
    private String role;                   // varchar(9) - PLAYER, ADMIN, TEAM_CAPTAIN
    private String displayName;            // varchar(80)
    private String bio;                    // longtext
    private String phone;                  // varchar(30)
    private String country;                // varchar(80)
    private LocalDate birthDate;           // date
    private String gender;                 // varchar(7) - MALE, FEMALE, UNKNOWN
    private boolean emailVerified;         // tinyint(4)
    private boolean isActive;              // tinyint(4)
    private LocalDateTime lastLoginAt;     // datetime
    private LocalDateTime createdAt;       // datetime
    private LocalDateTime updatedAt;       // datetime
    private String resetPasswordTokenHash; // varchar(64)
    private LocalDateTime resetPasswordExpiresAt; // datetime
    private boolean twoFactorEnabled;      // tinyint(4)
    private String twoFactorSecret;        // varchar(64)
    private LocalDateTime twoFactorEnabledAt; // datetime
    private Integer profileImageId;        // FK → images.image_id

    // Convenience constructor for basic user info
    public User(String username, String email, String passwordHash, String displayName) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.role = "PLAYER";
        this.gender = "UNKNOWN";
        this.emailVerified = false;
        this.isActive = true;
        this.twoFactorEnabled = false;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", displayName='" + displayName + '\'' +
                ", role='" + role + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}

