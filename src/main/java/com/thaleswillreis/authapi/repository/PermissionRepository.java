package com.thaleswillreis.authapi.repository;

import com.thaleswillreis.authapi.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {
}