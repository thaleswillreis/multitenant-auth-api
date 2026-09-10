package com.thaleswillreis.authapi.repository;

import com.thaleswillreis.authapi.model.Tenant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class TenantRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    void savesAndFindsTenantByIdentifier() {
        Tenant tenant = new Tenant("Acme Corp", "acme");
        tenantRepository.save(tenant);

        Optional<Tenant> found = tenantRepository.findByIdentifier("acme");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Acme Corp");
        assertThat(found.get().isActive()).isTrue();
    }

}