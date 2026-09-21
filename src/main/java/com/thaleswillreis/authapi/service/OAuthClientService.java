package com.thaleswillreis.authapi.service;

import com.thaleswillreis.authapi.dto.CreateOAuthClientRequest;
import com.thaleswillreis.authapi.dto.CreateOAuthClientResponse;
import com.thaleswillreis.authapi.model.OAuthClient;
import com.thaleswillreis.authapi.model.Permission;
import com.thaleswillreis.authapi.model.SecurityEventType;
import com.thaleswillreis.authapi.model.Tenant;
import com.thaleswillreis.authapi.repository.OAuthClientRepository;
import com.thaleswillreis.authapi.repository.PermissionRepository;
import com.thaleswillreis.authapi.repository.TenantRepository;
import com.thaleswillreis.authapi.security.SecurityAuditService;
import com.thaleswillreis.authapi.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;

@Service
public class OAuthClientService {

    private final OAuthClientRepository oAuthClientRepository;
    private final TenantRepository tenantRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityAuditService securityAuditService;

    public OAuthClientService(OAuthClientRepository oAuthClientRepository, TenantRepository tenantRepository,
            PermissionRepository permissionRepository, PasswordEncoder passwordEncoder,
            SecurityAuditService securityAuditService) {
        this.oAuthClientRepository = oAuthClientRepository;
        this.tenantRepository = tenantRepository;
        this.permissionRepository = permissionRepository;
        this.passwordEncoder = passwordEncoder;
        this.securityAuditService = securityAuditService;
    }

    @Transactional
    public CreateOAuthClientResponse create(CreateOAuthClientRequest request, String clientIp) {
        UUID tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant nao informado");
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant nao encontrado"));

        String clientId = "client-" + UUID.randomUUID();
        String rawSecret = generateSecret();
        String secretHash = passwordEncoder.encode(rawSecret);

        OAuthClient client = new OAuthClient(tenant, request.getName(), clientId, secretHash);

        Set<String> requestedPermissions = request.getPermissions() == null ? Set.of() : request.getPermissions();
        for (String permissionName : requestedPermissions) {
            Permission permission = permissionRepository.findByName(permissionName)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Permissao invalida: " + permissionName));
            client.addPermission(permission);
        }

        oAuthClientRepository.save(client);

        securityAuditService.record(tenantId, SecurityEventType.OAUTH_CLIENT_CREATED, clientId, true, clientIp);

        return new CreateOAuthClientResponse(client.getClientId(), rawSecret, client.getName(),
                client.getPermissionNames());
    }

    private String generateSecret() {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

}