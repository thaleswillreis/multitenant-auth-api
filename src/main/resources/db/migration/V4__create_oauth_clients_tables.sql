CREATE TABLE oauth_clients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    name VARCHAR(150) NOT NULL,
    client_id VARCHAR(100) NOT NULL UNIQUE,
    client_secret_hash VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_oauth_clients_tenant_id ON oauth_clients(tenant_id);

CREATE TABLE oauth_client_permissions (
    oauth_client_id UUID NOT NULL REFERENCES oauth_clients(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (oauth_client_id, permission_id)
);