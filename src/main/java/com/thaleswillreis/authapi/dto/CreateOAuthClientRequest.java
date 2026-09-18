package com.thaleswillreis.authapi.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Set;

public class CreateOAuthClientRequest {

    @NotBlank(message = "Nome e obrigatorio")
    private String name;

    private Set<String> permissions;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }

}