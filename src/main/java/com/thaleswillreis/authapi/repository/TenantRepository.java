package com.thaleswillreis.authapi.repository;

import com.thaleswillreis.authapi.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    Optional<Tenant> findByIdentifier(String identifier);

}