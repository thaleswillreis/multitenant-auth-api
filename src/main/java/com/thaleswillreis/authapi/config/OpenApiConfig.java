package com.thaleswillreis.authapi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI authApiOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Multitenant Auth API")
                        .description("Plataforma Multi-tenant de Autenticacao e Autorizacao (IAM/Auth API) - "
                                + "JWT, RBAC, OAuth2 Client Credentials, MFA (TOTP) e auditoria de seguranca.")
                        .version("v1")
                        .contact(new Contact()
                                .name("Thales Will S. Reis")
                                .url("https://github.com/thaleswillreis/multitenant-auth-api")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME_NAME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description(
                                        "Access token emitido por /api/auth/login, /api/auth/token ou /api/auth/verify-mfa")));
    }

}