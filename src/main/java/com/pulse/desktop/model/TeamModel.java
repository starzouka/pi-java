package com.pulse.desktop.model;

import java.time.LocalDateTime;

public class TeamModel {
    private Integer teamId;
    private String name;
    private String region;
    private Integer captainUserId;
    private String captainName;
    private LocalDateTime createdAt;

    public Integer getTeamId() {
        return teamId;
    }

    public void setTeamId(Integer teamId) {
        this.teamId = teamId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public Integer getCaptainUserId() {
        return captainUserId;
    }

    public void setCaptainUserId(Integer captainUserId) {
        this.captainUserId = captainUserId;
    }

    public String getCaptainName() {
        return captainName;
    }

    public void setCaptainName(String captainName) {
        this.captainName = captainName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
