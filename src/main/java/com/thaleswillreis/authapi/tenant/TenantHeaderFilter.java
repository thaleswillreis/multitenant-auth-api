package com.thaleswillreis.authapi.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class TenantHeaderFilter extends OncePerRequestFilter {

    private static final String TENANT_HEADER = "X-Tenant-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (!request.getRequestURI().equals("/api/auth/login")) {
            filterChain.doFilter(request, response);
            return;
        }

        String tenantHeader = request.getHeader(TENANT_HEADER);

        if (tenantHeader == null || tenantHeader.isBlank()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Header X-Tenant-Id e obrigatorio");
            return;
        }

        try {
            TenantContext.setCurrentTenant(UUID.fromString(tenantHeader));
            filterChain.doFilter(request, response);
        } catch (IllegalArgumentException ex) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Header X-Tenant-Id invalido");
        } finally {
            TenantContext.clear();
        }
    }

}