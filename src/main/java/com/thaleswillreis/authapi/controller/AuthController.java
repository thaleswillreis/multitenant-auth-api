package com.thaleswillreis.authapi.controller;

import com.thaleswillreis.authapi.dto.ClientCredentialsRequest;
import com.thaleswillreis.authapi.dto.ClientTokenResponse;
import com.thaleswillreis.authapi.dto.LoginRequest;
import com.thaleswillreis.authapi.dto.LoginResponse;
import com.thaleswillreis.authapi.dto.MfaVerifyRequest;
import com.thaleswillreis.authapi.dto.RefreshRequest;
import com.thaleswillreis.authapi.service.AuthService;
import com.thaleswillreis.authapi.util.ClientIpResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Autenticacao", description = "Login, logout, refresh de token, client credentials e verificacao de MFA")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Login com email e senha", description = "Retorna os tokens de acesso, ou um desafio de MFA se o usuario tiver ativado.", parameters = @Parameter(name = "X-Tenant-Id", in = ParameterIn.HEADER, required = true, description = "UUID do tenant ao qual o usuario pertence"))
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return authService.login(request, ClientIpResolver.resolve(httpRequest));
    }

    @PostMapping("/verify-mfa")
    @SecurityRequirements
    @Operation(summary = "Conclui o login verificando o codigo TOTP do desafio de MFA")
    public LoginResponse verifyMfa(@Valid @RequestBody MfaVerifyRequest request, HttpServletRequest httpRequest) {
        return authService.verifyMfa(request, ClientIpResolver.resolve(httpRequest));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revoga o access token atual (logout efetivo via blacklist)")
    public void logout(@RequestHeader("Authorization") String authorizationHeader, HttpServletRequest httpRequest) {
        authService.logout(authorizationHeader, ClientIpResolver.resolve(httpRequest));
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(summary = "Troca um refresh token valido por um novo par de tokens (rotacao)")
    public LoginResponse refresh(@Valid @RequestBody RefreshRequest request, HttpServletRequest httpRequest) {
        return authService.refresh(request, ClientIpResolver.resolve(httpRequest));
    }

    @PostMapping("/token")
    @SecurityRequirements
    @Operation(summary = "Fluxo OAuth2 Client Credentials para aplicacoes/microsservicos")
    public ClientTokenResponse clientCredentialsToken(@Valid @RequestBody ClientCredentialsRequest request,
            HttpServletRequest httpRequest) {
        return authService.clientCredentials(request, ClientIpResolver.resolve(httpRequest));
    }

}