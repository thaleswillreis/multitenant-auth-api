package com.thaleswillreis.authapi.dto;

public class ClientTokenResponse {

    private final String accessToken;
    private final String tokenType;
    private final long expiresIn;

    public ClientTokenResponse(String accessToken, String tokenType, long expiresIn) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

}