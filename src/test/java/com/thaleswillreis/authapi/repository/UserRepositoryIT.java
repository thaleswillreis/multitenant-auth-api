package com.thaleswillreis.authapi.repository;

import com.thaleswillreis.authapi.model.Tenant;
import com.thaleswillreis.authapi.model.User;
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
class UserRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void savesAndFindsUserByTenantAndEmail() {
        Tenant tenant = tenantRepository.save(new Tenant("Acme Corp", "acme"));
        userRepository.save(new User(tenant, "joao@acme.com", "hashed-password"));

        Optional<User> found = userRepository.findByTenantIdAndEmail(tenant.getId(), "joao@acme.com");

        assertThat(found).isPresent();
        assertThat(found.get().isActive()).isTrue();
        assertThat(found.get().getTenant().getIdentifier()).isEqualTo("acme");
    }

    @Test
    void allowsSameEmailAcrossDifferentTenants() {
        Tenant tenantA = tenantRepository.save(new Tenant("Acme Corp", "acme"));
        Tenant tenantB = tenantRepository.save(new Tenant("Beta Corp", "beta"));

        userRepository.save(new User(tenantA, "joao@example.com", "hash-a"));
        userRepository.save(new User(tenantB, "joao@example.com", "hash-b"));

        assertThat(userRepository.findByTenantIdAndEmail(tenantA.getId(), "joao@example.com")).isPresent();
        assertThat(userRepository.findByTenantIdAndEmail(tenantB.getId(), "joao@example.com")).isPresent();
    }

}