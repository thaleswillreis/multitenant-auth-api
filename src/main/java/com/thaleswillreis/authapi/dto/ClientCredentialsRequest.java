package com.thaleswillreis.authapi.dto;

import jakarta.validation.constraints.NotBlank;

public class ClientCredentialsRequest {

    @NotBlank(message = "clientId e obrigatorio")
    private String clientId;

    @NotBlank(message = "clientSecret e obrigatorio")
    private String clientSecret;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

}