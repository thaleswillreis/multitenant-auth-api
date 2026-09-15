package com.thaleswillreis.authapi.dto;

import com.thaleswillreis.authapi.model.User;

import java.time.OffsetDateTime;
import java.util.UUID;

public class UserResponse {

    private final UUID id;
    private final String email;
    private final boolean active;
    private final OffsetDateTime createdAt;

    public UserResponse(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.active = user.isActive();
        this.createdAt = user.getCreatedAt();
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public boolean isActive() {
        return active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

}