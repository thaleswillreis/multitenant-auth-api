INSERT INTO permissions (name, description) VALUES
    ('OAUTH_CLIENT_READ', 'Permite visualizar clientes OAuth2'),
    ('OAUTH_CLIENT_WRITE', 'Permite criar clientes OAuth2');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ADMIN' AND p.name IN ('OAUTH_CLIENT_READ', 'OAUTH_CLIENT_WRITE');