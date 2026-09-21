package com.thaleswillreis.authapi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "security_audit_events")
public class SecurityAuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private SecurityEventType eventType;

    @Column(length = 255)
    private String subject;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected SecurityAuditEvent() {
        // exigido pelo JPA
    }

    public SecurityAuditEvent(UUID tenantId, SecurityEventType eventType, String subject, boolean success,
            String ipAddress) {
        this.tenantId = tenantId;
        this.eventType = eventType;
        this.subject = subject;
        this.success = success;
        this.ipAddress = ipAddress;
        this.createdAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public SecurityEventType getEventType() {
        return eventType;
    }

    public String getSubject() {
        return subject;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

}