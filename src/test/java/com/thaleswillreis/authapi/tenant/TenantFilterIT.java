package com.thaleswillreis.authapi.tenant;

import com.thaleswillreis.authapi.model.Tenant;
import com.thaleswillreis.authapi.model.User;
import com.thaleswillreis.authapi.repository.TenantRepository;
import com.thaleswillreis.authapi.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(TenantFilterAspect.class)
@ImportAutoConfiguration(AopAutoConfiguration.class)
class TenantFilterIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void onlyReturnsUsersFromCurrentTenantWhenFilterIsActive() {
        Tenant tenantA = tenantRepository.save(new Tenant("Acme Corp", "acme"));
        Tenant tenantB = tenantRepository.save(new Tenant("Beta Corp", "beta"));

        userRepository.save(new User(tenantA, "joao@acme.com", "hash-a"));
        userRepository.save(new User(tenantB, "maria@beta.com", "hash-b"));

        TenantContext.setCurrentTenant(tenantA.getId());

        List<User> visibleUsers = userRepository.findAll();

        assertThat(visibleUsers).hasSize(1);
        assertThat(visibleUsers.get(0).getEmail()).isEqualTo("joao@acme.com");
    }

    @Test
    void returnsAllUsersWhenNoTenantIsSetInContext() {
        Tenant tenantA = tenantRepository.save(new Tenant("Acme Corp", "acme"));
        Tenant tenantB = tenantRepository.save(new Tenant("Beta Corp", "beta"));

        userRepository.save(new User(tenantA, "joao@acme.com", "hash-a"));
        userRepository.save(new User(tenantB, "maria@beta.com", "hash-b"));

        List<User> allUsers = userRepository.findAll();

        assertThat(allUsers).hasSize(2);
    }

}