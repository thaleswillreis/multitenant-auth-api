CREATE TABLE security_audit_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID,
    event_type VARCHAR(50) NOT NULL,
    subject VARCHAR(255),
    success BOOLEAN NOT NULL,
    ip_address VARCHAR(45),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_security_audit_events_tenant_id ON security_audit_events(tenant_id);
CREATE INDEX idx_security_audit_events_event_type ON security_audit_events(event_type);