package com.thaleswillreis.authapi.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrentUserResolverTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolvesUuidFromAuthenticatedPrincipal() {
        UUID userId = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId.toString(), null, List.of()));

        assertThat(CurrentUserResolver.resolve()).isEqualTo(userId);
    }

    @Test
    void rejectsWhenNoAuthenticationPresent() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(CurrentUserResolver::resolve)
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("nao autenticado");
    }

    @Test
    void rejectsWhenPrincipalIsNotAValidUuid() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("nao-e-um-uuid", null, List.of()));

        assertThatThrownBy(CurrentUserResolver::resolve)
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("nao autenticado");
    }

}