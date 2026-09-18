package com.thaleswillreis.authapi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Table(name = "oauth_clients")
public class OAuthClient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "client_id", nullable = false, unique = true, length = 100)
    private String clientId;

    @Column(name = "client_secret_hash", nullable = false, length = 255)
    private String clientSecretHash;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "oauth_client_permissions", joinColumns = @JoinColumn(name = "oauth_client_id"), inverseJoinColumns = @JoinColumn(name = "permission_id"))
    private Set<Permission> permissions = new HashSet<>();

    protected OAuthClient() {
        // exigido pelo JPA
    }

    public OAuthClient(Tenant tenant, String name, String clientId, String clientSecretHash) {
        this.tenant = tenant;
        this.name = name;
        this.clientId = clientId;
        this.clientSecretHash = clientSecretHash;
        this.active = true;
        this.createdAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public String getName() {
        return name;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecretHash() {
        return clientSecretHash;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public void addPermission(Permission permission) {
        this.permissions.add(permission);
    }

    public Set<String> getPermissionNames() {
        Set<String> names = new HashSet<>();
        for (Permission permission : permissions) {
            names.add(permission.getName());
        }
        return names;
    }

}