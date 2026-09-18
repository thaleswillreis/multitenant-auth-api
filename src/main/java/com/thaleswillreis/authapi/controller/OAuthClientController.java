package com.thaleswillreis.authapi.controller;

import com.thaleswillreis.authapi.dto.CreateOAuthClientRequest;
import com.thaleswillreis.authapi.dto.CreateOAuthClientResponse;
import com.thaleswillreis.authapi.service.OAuthClientService;
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
public class OAuthClientController {

    private final OAuthClientService oAuthClientService;

    public OAuthClientController(OAuthClientService oAuthClientService) {
        this.oAuthClientService = oAuthClientService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('OAUTH_CLIENT_WRITE')")
    public CreateOAuthClientResponse create(@Valid @RequestBody CreateOAuthClientRequest request) {
        return oAuthClientService.create(request);
    }

}