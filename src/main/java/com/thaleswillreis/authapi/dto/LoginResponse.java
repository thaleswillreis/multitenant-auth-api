package com.thaleswillreis.authapi.dto;

public class LoginResponse {

    private final String accessToken;
    private final String refreshToken;
    private final String tokenType;
    private final long expiresIn;
    private final boolean mfaRequired;
    private final String challengeToken;

    public LoginResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {
        this(accessToken, refreshToken, tokenType, expiresIn, false, null);
    }

    private LoginResponse(String accessToken, String refreshToken, String tokenType, long expiresIn,
            boolean mfaRequired, String challengeToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.mfaRequired = mfaRequired;
        this.challengeToken = challengeToken;
    }

    public static LoginResponse mfaChallenge(String challengeToken) {
        return new LoginResponse(null, null, null, 0, true, challengeToken);
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public boolean isMfaRequired() {
        return mfaRequired;
    }

    public String getChallengeToken() {
        return challengeToken;
    }

}