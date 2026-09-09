# Multitenant Auth API

Plataforma Multi-tenant de Autenticação e Autorização (IAM/Auth API) — serviço central de identidade responsável por emissão de tokens, gerenciamento de usuários, controle de acesso baseado em papéis (RBAC) e login seguro para múltiplas aplicações, microsserviços ou clientes.

## Stack

- Java 21
- Spring Boot 3 (Web, Security)
- OAuth2 / JWT
- PostgreSQL
- Redis (blacklist/invalidação de tokens)
- Maven
- JUnit

## Status do Projeto

Em desenvolvimento — projeto de portfólio.

## Roteiro do Projeto

- [x] **Fase 0 — Fundamentos e Setup**
  - [x] Tarefa 0.1 — Setup inicial do repositório e projeto Maven
  - [ ] Tarefa 0.2 — Docker Compose (PostgreSQL + Redis)
- [ ] **Fase 1 — Multi-tenancy e Modelagem Base**
- [ ] **Fase 2 — Identidade e Cadastro de Usuários**
- [ ] **Fase 3 — Autenticação (JWT)**
- [ ] **Fase 4 — Autorização (RBAC)**
- [ ] **Fase 5 — Gestão de Sessão e Revogação (Redis)**
- [ ] **Fase 6 — OAuth2 para Aplicações Clientes**
- [ ] **Fase 7 — Segurança Avançada**
- [ ] **Fase 8 — Testes**
- [ ] **Fase 9 — Observabilidade e Documentação**
- [ ] **Fase 10 — Finalização**

## Como rodar localmente

```bash
./mvnw spring-boot:run
```

Depois, acesse: `http://localhost:8080/health`

## Licença

Este projeto está licenciado sob a licença MIT — veja o arquivo [LICENSE](LICENSE) para detalhes.