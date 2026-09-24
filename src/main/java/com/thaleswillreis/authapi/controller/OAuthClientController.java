package com.thaleswillreis.authapi.controller;

import com.thaleswillreis.authapi.dto.CreateOAuthClientRequest;
import com.thaleswillreis.authapi.dto.CreateOAuthClientResponse;
import com.thaleswillreis.authapi.service.OAuthClientService;
import com.thaleswillreis.authapi.util.ClientIpResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/oauth-clients")
@Tag(name = "Clientes OAuth2", description = "Registro de aplicacoes/microsservicos consumidores (Client Credentials)")
public class OAuthClientController {

    private final OAuthClientService oAuthClientService;

    public OAuthClientController(OAuthClientService oAuthClientService) {
        this.oAuthClientService = oAuthClientService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('OAUTH_CLIENT_WRITE')")
    @Operation(summary = "Registra um novo cliente OAuth2 para o tenant atual", description = "O client_secret retornado so e exibido nesta resposta - guarde-o com seguranca.")
    public CreateOAuthClientResponse create(@Valid @RequestBody CreateOAuthClientRequest request,
            HttpServletRequest httpRequest) {
        return oAuthClientService.create(request, ClientIpResolver.resolve(httpRequest));
    }

}