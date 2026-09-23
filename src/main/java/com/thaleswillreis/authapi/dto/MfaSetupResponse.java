package com.thaleswillreis.authapi.dto;

public class MfaSetupResponse {

    private final String secret;
    private final String otpAuthUri;
    private final String qrCodeImage;

    public MfaSetupResponse(String secret, String otpAuthUri, String qrCodeImage) {
        this.secret = secret;
        this.otpAuthUri = otpAuthUri;
        this.qrCodeImage = qrCodeImage;
    }

    public String getSecret() {
        return secret;
    }

    public String getOtpAuthUri() {
        return otpAuthUri;
    }

    public String getQrCodeImage() {
        return qrCodeImage;
    }

}