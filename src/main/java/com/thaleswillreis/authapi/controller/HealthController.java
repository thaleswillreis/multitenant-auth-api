package com.thaleswillreis.authapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Saude", description = "Verificacao basica de disponibilidade da aplicacao")
public class HealthController {

    @GetMapping("/health")
    @SecurityRequirements
    @Operation(summary = "Verifica se a aplicacao esta no ar")
    public String health() {
        return "multitenant-auth-api is up";
    }

}