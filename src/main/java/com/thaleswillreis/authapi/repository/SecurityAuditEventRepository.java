package com.thaleswillreis.authapi.repository;

import com.thaleswillreis.authapi.model.SecurityAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SecurityAuditEventRepository extends JpaRepository<SecurityAuditEvent, UUID> {
}