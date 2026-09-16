package com.thaleswillreis.authapi.service;

import com.thaleswillreis.authapi.dto.CreateUserRequest;
import com.thaleswillreis.authapi.dto.UserResponse;
import com.thaleswillreis.authapi.model.Tenant;
import com.thaleswillreis.authapi.repository.TenantRepository;
import com.thaleswillreis.authapi.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Constructor;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void hashesPasswordBeforeSavingUser() throws Exception {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = newTenant(tenantId, "Acme Corp", "acme");

        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("joao@acme.com");
        request.setPassword("plain-text-password");

        com.thaleswillreis.authapi.tenant.TenantContext.setCurrentTenant(tenantId);

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(userRepository.findByTenantIdAndEmail(tenantId, "joao@acme.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("plain-text-password")).thenReturn("$2a$10$hashed-value-simulated");

        UserService userService = new UserService(userRepository, tenantRepository, passwordEncoder);
        UserResponse response = userService.create(request);

        ArgumentCaptor<com.thaleswillreis.authapi.model.User> userCaptor =
                ArgumentCaptor.forClass(com.thaleswillreis.authapi.model.User.class);
        verify(userRepository).save(userCaptor.capture());

        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("$2a$10$hashed-value-simulated");
        assertThat(userCaptor.getValue().getPasswordHash()).isNotEqualTo("plain-text-password");
        assertThat(response.getEmail()).isEqualTo("joao@acme.com");

        com.thaleswillreis.authapi.tenant.TenantContext.clear();
    }

    private Tenant newTenant(UUID id, String name, String identifier) throws Exception {
        Tenant tenant = new Tenant(name, identifier);
        Constructor<Tenant> ignored = Tenant.class.getDeclaredConstructor();
        var idField = Tenant.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(tenant, id);
        return tenant;
    }

}