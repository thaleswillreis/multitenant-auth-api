package com.thaleswillreis.authapi.dto;

import java.util.Set;

public class CreateOAuthClientResponse {

    private final String clientId;
    private final String clientSecret;
    private final String name;
    private final Set<String> permissions;

    public CreateOAuthClientResponse(String clientId, String clientSecret, String name, Set<String> permissions) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.name = name;
        this.permissions = permissions;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getName() {
        return name;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

}