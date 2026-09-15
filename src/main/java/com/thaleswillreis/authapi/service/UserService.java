package com.thaleswillreis.authapi.service;

import com.thaleswillreis.authapi.dto.CreateUserRequest;
import com.thaleswillreis.authapi.dto.UserResponse;
import com.thaleswillreis.authapi.model.Tenant;
import com.thaleswillreis.authapi.model.User;
import com.thaleswillreis.authapi.repository.TenantRepository;
import com.thaleswillreis.authapi.repository.UserRepository;
import com.thaleswillreis.authapi.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;

    public UserService(UserRepository userRepository, TenantRepository tenantRepository) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
    }

    public UserResponse create(CreateUserRequest request) {
        Tenant tenant = resolveCurrentTenant();

        userRepository.findByTenantIdAndEmail(tenant.getId(), request.getEmail())
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Ja existe um usuario com esse email neste tenant");
                });

        // ATENCAO: password armazenado como texto puro - sera corrigido na Tarefa 2.2 (hashing com BCrypt)
        User user = new User(tenant, request.getEmail(), request.getPassword());
        userRepository.save(user);

        return new UserResponse(user);
    }

    public List<UserResponse> listAll() {
        return userRepository.findAll().stream()
                .map(UserResponse::new)
                .toList();
    }

    public UserResponse getById(UUID id) {
        UUID currentTenantId = TenantContext.getCurrentTenant();

        User user = userRepository.findById(id)
                .filter(u -> u.getTenant().getId().equals(currentTenantId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario nao encontrado"));

        return new UserResponse(user);
    }

    private Tenant resolveCurrentTenant() {
        UUID tenantId = TenantContext.getCurrentTenant();
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant nao encontrado"));
    }

}