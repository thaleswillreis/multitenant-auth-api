# Roteiro do Projeto — Multitenant Auth API

> Roteiro dividido em Fases (Épicos) e Tarefas.
> As tarefas das fases 1 em diante são um planejamento preliminar e podem ser ajustadas conforme o projeto evolui e conforme as necessidades de ajustes no que já foi feito.

## Fase 0 — Fundamentos e Setup

- [x] **Tarefa 0.1** — Setup inicial do repositório e projeto Maven (Java 21, Spring Boot 3, estrutura de pacotes, README com roteiro, LICENSE MIT, Maven Wrapper)
- [x] **Tarefa 0.2** — Docker Compose (PostgreSQL + Redis)
- [ ] **Tarefa 0.3** — Configuração de perfis de ambiente (application.yml para dev/test)
- [ ] **Tarefa 0.4** — Setup base de testes (JUnit + Testcontainers)

## Fase 1 — Multi-tenancy e Modelagem Base

- [ ] **Tarefa 1.1** — Modelagem da entidade `Tenant`
- [ ] **Tarefa 1.2** — Modelagem da entidade `User` vinculada ao tenant (coluna `tenant_id`)
- [ ] **Tarefa 1.3** — Filtro automático de isolamento por tenant (Hibernate Filter)

## Fase 2 — Identidade e Cadastro de Usuários

- [ ] **Tarefa 2.1** — CRUD de usuários (criação, listagem, consulta)
- [ ] **Tarefa 2.2** — Hashing de senha (BCrypt/Argon2)
- [ ] **Tarefa 2.3** — Validações de entrada (Bean Validation)

## Fase 3 — Autenticação (JWT)

- [ ] **Tarefa 3.1** — Configuração base do Spring Security
- [ ] **Tarefa 3.2** — Endpoint de login com emissão de access token + refresh token
- [ ] **Tarefa 3.3** — Filtro de validação de token nas requisições

## Fase 4 — Autorização (RBAC)

- [ ] **Tarefa 4.1** — Modelagem de `Role` e `Permission`
- [ ] **Tarefa 4.2** — Checagem de acesso por papel (`@PreAuthorize`)
- [ ] **Tarefa 4.3** — Escopo das regras de autorização por tenant

## Fase 5 — Gestão de Sessão e Revogação (Redis)

- [ ] **Tarefa 5.1** — Integração com Redis
- [ ] **Tarefa 5.2** — Blacklist de tokens (logout efetivo)
- [ ] **Tarefa 5.3** — Rotação de refresh token

## Fase 6 — OAuth2 para Aplicações Clientes

- [ ] **Tarefa 6.1** — Modelagem de clientes (client_id/client_secret)
- [ ] **Tarefa 6.2** — Fluxo client credentials para microsserviços consumidores

## Fase 7 — Segurança Avançada

- [ ] **Tarefa 7.1** — Rate limiting
- [ ] **Tarefa 7.2** — Proteção contra brute force
- [ ] **Tarefa 7.3** — Auditoria de eventos de segurança (login, falha de login, troca de senha)
- [ ] **Tarefa 7.4** — MFA (extensão opcional)

## Fase 8 — Testes

- [ ] **Tarefa 8.1** — Testes unitários (JUnit)
- [ ] **Tarefa 8.2** — Testes de integração com Testcontainers (Postgres + Redis reais)
- [ ] **Tarefa 8.3** — Testes de segurança dos endpoints

## Fase 9 — Observabilidade e Documentação

- [ ] **Tarefa 9.1** — Logs estruturados
- [ ] **Tarefa 9.2** — Métricas
- [ ] **Tarefa 9.3** — Documentação da API (OpenAPI/Swagger)

## Fase 10 — Finalização

- [ ] **Tarefa 10.1** — CI/CD (GitHub Actions)
- [ ] **Tarefa 10.2** — Docker multi-stage
- [ ] **Tarefa 10.3** — README completo e final
- [ ] **Tarefa 10.4** — Deploy didático
