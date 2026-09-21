package com.thaleswillreis.authapi.security;

import com.thaleswillreis.authapi.model.SecurityAuditEvent;
import com.thaleswillreis.authapi.model.SecurityEventType;
import com.thaleswillreis.authapi.repository.SecurityAuditEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SecurityAuditService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityAuditService.class);

    private final SecurityAuditEventRepository repository;

    public SecurityAuditService(SecurityAuditEventRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UUID tenantId, SecurityEventType eventType, String subject, boolean success, String ipAddress) {
        try {
            repository.save(new SecurityAuditEvent(tenantId, eventType, subject, success, ipAddress));
        } catch (RuntimeException ex) {
            LOGGER.error("Falha ao registrar evento de auditoria [{}] para o assunto [{}]", eventType, subject, ex);
        }
    }

}