package com.thaleswillreis.authapi.security;

import com.thaleswillreis.authapi.tenant.TenantContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;

    public JwtAuthenticationFilter(JwtService jwtService, TokenBlacklistService tokenBlacklistService) {
        this.jwtService = jwtService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            Claims claims = jwtService.parseToken(token);

            String tokenType = claims.get("type", String.class);
            if (!"access".equals(tokenType) && !"client".equals(tokenType)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token nao e um access token valido");
                return;
            }

            if (claims.getId() != null && tokenBlacklistService.isBlacklisted(claims.getId())) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token revogado");
                return;
            }

            String tenantIdClaim = claims.get("tenant_id", String.class);
            if (tenantIdClaim == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token sem tenant_id valido");
                return;
            }

            UUID tenantId = UUID.fromString(tenantIdClaim);
            String userId = claims.getSubject();

            TenantContext.setCurrentTenant(tenantId);

            List<String> permissions = claims.get("permissions", List.class);
            List<SimpleGrantedAuthority> authorities = permissions == null
                    ? List.of()
                    : permissions.stream().map(SimpleGrantedAuthority::new).toList();

            var authentication = new UsernamePasswordAuthenticationToken(userId, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException ex) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token invalido ou expirado");
        } finally {
            TenantContext.clear();
        }
    }

}