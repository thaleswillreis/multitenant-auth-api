package com.thaleswillreis.authapi.repository;

import com.thaleswillreis.authapi.model.OAuthClient;
import com.thaleswillreis.authapi.model.Permission;
import com.thaleswillreis.authapi.model.Tenant;
import com.thaleswillreis.authapi.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(com.thaleswillreis.authapi.tenant.TenantFilterAspect.class)
@ImportAutoConfiguration(AopAutoConfiguration.class)
class OAuthClientRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private OAuthClientRepository oAuthClientRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void savesAndFindsClientByClientId() {
        Tenant tenant = tenantRepository.save(new Tenant("Acme Corp", "acme-oauth-it"));
        Permission userRead = permissionRepository.findAll().stream()
                .filter(p -> p.getName().equals("USER_READ"))
                .findFirst()
                .orElseThrow();

        OAuthClient client = new OAuthClient(tenant, "Billing Service", "billing-service-acme", "hashed-secret");
        client.addPermission(userRead);
        oAuthClientRepository.save(client);

        Optional<OAuthClient> found = oAuthClientRepository.findByClientId("billing-service-acme");

        assertThat(found).isPresent();
        assertThat(found.get().isActive()).isTrue();
        assertThat(found.get().getPermissionNames()).containsExactly("USER_READ");
    }

    @Test
    void onlyReturnsClientsFromCurrentTenantWhenFilterIsActive() {
        Tenant tenantA = tenantRepository.save(new Tenant("Acme Corp", "acme-oauth-scope-it"));
        Tenant tenantB = tenantRepository.save(new Tenant("Beta Corp", "beta-oauth-scope-it"));

        oAuthClientRepository.save(new OAuthClient(tenantA, "Service A", "service-a", "hash-a"));
        oAuthClientRepository.save(new OAuthClient(tenantB, "Service B", "service-b", "hash-b"));

        TenantContext.setCurrentTenant(tenantA.getId());

        List<OAuthClient> visibleClients = oAuthClientRepository.findAll();

        assertThat(visibleClients).hasSize(1);
        assertThat(visibleClients.get(0).getClientId()).isEqualTo("service-a");
    }

}