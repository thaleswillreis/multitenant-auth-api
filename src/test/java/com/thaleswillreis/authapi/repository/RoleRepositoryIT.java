package com.thaleswillreis.authapi.repository;

import com.thaleswillreis.authapi.model.Role;
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
class RoleRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void adminRoleIsSeededWithAllPermissions() {
        Optional<Role> admin = roleRepository.findByName("ADMIN");

        assertThat(admin).isPresent();
        assertThat(admin.get().getPermissions()).extracting("name")
                .containsExactlyInAnyOrder("USER_READ", "USER_WRITE", "USER_DELETE");
    }

    @Test
    void memberRoleIsSeededWithReadOnlyPermission() {
        Optional<Role> member = roleRepository.findByName("MEMBER");

        assertThat(member).isPresent();
        assertThat(member.get().getPermissions()).extracting("name")
                .containsExactly("USER_READ");
    }

}