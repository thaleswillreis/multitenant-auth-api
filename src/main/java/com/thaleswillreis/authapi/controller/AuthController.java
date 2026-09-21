package com.thaleswillreis.authapi.controller;

import com.thaleswillreis.authapi.dto.ClientCredentialsRequest;
import com.thaleswillreis.authapi.dto.ClientTokenResponse;
import com.thaleswillreis.authapi.dto.LoginRequest;
import com.thaleswillreis.authapi.dto.LoginResponse;
import com.thaleswillreis.authapi.dto.RefreshRequest;
import com.thaleswillreis.authapi.service.AuthService;
import com.thaleswillreis.authapi.util.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return authService.login(request, ClientIpResolver.resolve(httpRequest));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestHeader("Authorization") String authorizationHeader, HttpServletRequest httpRequest) {
        authService.logout(authorizationHeader, ClientIpResolver.resolve(httpRequest));
    }

    @PostMapping("/refresh")
    public LoginResponse refresh(@Valid @RequestBody RefreshRequest request, HttpServletRequest httpRequest) {
        return authService.refresh(request, ClientIpResolver.resolve(httpRequest));
    }

    @PostMapping("/token")
    public ClientTokenResponse clientCredentialsToken(@Valid @RequestBody ClientCredentialsRequest request,
            HttpServletRequest httpRequest) {
        return authService.clientCredentials(request, ClientIpResolver.resolve(httpRequest));
    }

}