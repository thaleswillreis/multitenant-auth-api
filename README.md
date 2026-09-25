# Multitenant Auth API

![CI](https://github.com/thaleswillreis/multitenant-auth-api/actions/workflows/ci.yml/badge.svg)
![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Java](https://img.shields.io/badge/Java-21-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)

Plataforma de Identidade e Acesso (IAM) multi-tenant construída em Java/Spring Boot, cobrindo autenticação via JWT, autorização baseada em papéis (RBAC), OAuth2 Client Credentials para integrações máquina-a-máquina, autenticação multifator (TOTP), e um conjunto completo de recursos de segurança avançada, testes automatizados, observabilidade e CI/CD.

>[!IMPORTANT]
>Este é um projeto de portfólio, construído de forma incremental e deliberada, fase por fase — cada decisão de arquitetura ao longo do caminho foi pensada e documentada, não apenas implementada.

## Sumário

- [Tecnologias Utilizadas](#tecnologias-utilizadas)
- [Decisões de Arquitetura](#decisões-de-arquitetura)
- [Fases do Projeto](#fases-do-projeto)
- [Como Reproduzir](#como-reproduzir)
- [Problemas Enfrentados](#problemas-enfrentados)
- [Resultados Obtidos](#resultados-obtidos)
- [Melhorias Futuras / Próximos Passos](#melhorias-futuras--próximos-passos)
- [Licença](#licença)
- [Contribuindo](#contribuindo)

## Tecnologias Utilizadas

- **Linguagem/Runtime**: Java 21;
- **Framework**: Spring Boot 3.3.4 (Web, Security, Data JPA, AOP, Validation, Actuator);
- **Banco de dados**: PostgreSQL 16, com migrations via Flyway;
- **Cache/Sessão**: Redis 7 (blacklist de tokens, rate limiting, tentativas de login);
- **Autenticação**: JWT assinado com RS256 (JJWT 0.13.0);
- **Autorização**: RBAC declarativo (`@PreAuthorize`);
- **MFA**: TOTP (`dev.samstevens.totp`) com QR Code (ZXing embutido);
- **Rate Limiting**: Bucket4j (token bucket) com backend distribuído no Redis;
- **Observabilidade**: Micrometer + Prometheus (métricas), Logback + Logstash Encoder (logs estruturados em JSON), correlation ID por requisição;
- **Documentação de API**: springdoc-openapi (Swagger UI);
- **Testes**: JUnit 5, Mockito, Testcontainers (Postgres + Redis reais em integração), JaCoCo (cobertura);
- **Build**: Maven (com Maven Wrapper);
- **Containerização**: Docker (build multi-stage) e Docker Compose;
- **CI/CD**: GitHub Actions.

## Decisões de Arquitetura

Algumas das decisões mais relevantes tomadas ao longo do projeto, com a motivação por trás de cada uma:

- **Multi-tenancy por coluna `tenant_id`** (schema único e compartilhado), com isolamento automático via Hibernate Filter interceptado por AOP — simplicidade operacional em troca de isolamento total de schema-per-tenant.
- **UUID como chave primária** em todas as entidades — evita previsibilidade sequencial de IDs.
- **JWT assinado com RS256**, par de chaves RSA gerado em memória a cada inicialização — elimina a necessidade de gerenciar um segredo simétrico compartilhado, ao custo de invalidar sessões ativas a cada restart (uma simplificação deliberada e documentada, aceitável para um projeto de portfólio).
- **Tenant identificado a partir do JWT**, não mais do header, após a autenticação — impede que o cliente manipule o tenant de uma requisição autenticada trocando um header.
- **Blacklist de tokens via Redis com TTL igual ao tempo restante do token** — logout efetivo sem precisar armazenar nem consultar todos os tokens já emitidos.
- **RBAC com papéis fixos** (`ADMIN`, `MEMBER`) e permissões granulares — simples de raciocinar, com checagem declarativa via `@PreAuthorize`.
- **Clientes OAuth2 escopados por tenant** — cada tenant gerencia suas próprias integrações máquina-a-máquina, sem um conceito de "cliente global".
- **Segredo do MFA criptografado em repouso** (AES-256/GCM) — nunca armazenado em texto puro, mesmo precisando ser decifrado a cada verificação de código.
- **Rate limiting com token bucket** (Bucket4j + Redis) — permite rajadas curtas controladas, com estado compartilhado entre múltiplas instâncias da aplicação.
- **Auditoria de segurança e métricas conectadas num único ponto de integração** (`SecurityAuditService`) — todo evento já auditado (login, MFA, criação de cliente OAuth2, etc.) automaticamente também vira uma métrica Prometheus, sem duplicar código em cada `Service`.

## Fases do Projeto

### Fase 0 — Fundamentos e Setup
- **0.1** Setup inicial do repositório e projeto Maven (Java 21, estrutura de pacotes, Maven Wrapper)
- **0.2** Docker Compose com PostgreSQL e Redis para desenvolvimento local
- **0.3** Perfis de ambiente (`dev`, `test`) via `application-*.yml`
- **0.4** Setup base de testes com JUnit 5 e Testcontainers

### Fase 1 — Multi-tenancy e Modelagem Base
- **1.1** Modelagem da entidade `Tenant`
- **1.2** Modelagem da entidade `User`, vinculada ao tenant via `tenant_id`
- **1.3** Filtro automático de isolamento por tenant, via Hibernate Filter interceptado por Spring AOP

### Fase 2 — Identidade e Cadastro de Usuários
- **2.1** CRUD de usuários (criação, listagem, consulta por ID)
- **2.2** Hashing de senha com BCrypt
- **2.3** Validações de entrada com Bean Validation

### Fase 3 — Autenticação (JWT)
- **3.1** Configuração base do Spring Security (stateless, sem sessão)
- **3.2** Endpoint de login com emissão de access token e refresh token (RS256)
- **3.3** Filtro de validação de token em toda requisição autenticada

### Fase 4 — Autorização (RBAC)
- **4.1** Modelagem de `Role` e `Permission`, com seed de papéis padrão
- **4.2** Checagem de acesso por permissão via `@PreAuthorize`
- **4.3** Escopo das regras de autorização isolado por tenant

### Fase 5 — Gestão de Sessão e Revogação (Redis)
- **5.1** Integração com Redis
- **5.2** Blacklist de tokens para logout efetivo
- **5.3** Rotação de refresh token (uso único, invalidação do token anterior)

### Fase 6 — OAuth2 para Aplicações Clientes
- **6.1** Modelagem de clientes OAuth2 (`client_id`/`client_secret`), escopados por tenant
- **6.2** Fluxo Client Credentials completo, com endpoint administrativo de registro de clientes

### Fase 7 — Segurança Avançada
- **7.1** Rate limiting por IP (token bucket via Bucket4j + Redis)
- **7.2** Proteção contra brute force (bloqueio de conta após tentativas falhas consecutivas)
- **7.3** Auditoria de eventos de segurança (login, logout, refresh, MFA, criação de clientes OAuth2)
- **7.4** Autenticação multifator (MFA) via TOTP, com QR Code e segredo criptografado em repouso

### Fase 8 — Testes
- **8.1** Testes unitários (JUnit + Mockito)
- **8.2** Testes de integração com Testcontainers (Postgres e Redis reais)
- **8.3** Varredura de segurança sistemática (adulteração de JWT, isolamento cross-tenant, vazamento de dados sensíveis, headers de segurança)

### Fase 9 — Observabilidade e Documentação
- **9.1** Logs estruturados em JSON, com correlation ID propagado por requisição
- **9.2** Métricas via Spring Boot Actuator e Prometheus, incluindo métricas de negócio conectadas à auditoria de segurança
- **9.3** Documentação interativa da API via OpenAPI/Swagger

### Fase 10 — Finalização
- **10.1** Pipeline de CI via GitHub Actions (build, testes e cobertura a cada push)
- **10.2** Imagem Docker multi-stage (build enxuto, usuário não-root) e Docker Compose completo
- **10.3** Este README
- **10.4** Guia de deploy documentado — veja [`DEPLOY.md`](./DEPLOY.md)

## Como Reproduzir

### Pré-requisitos

- Java 21
- Docker e Docker Compose
- Git
- (Opcional) Maven — o projeto já inclui o Maven Wrapper (`./mvnw`)

### Configuração inicial

```bash
git clone https://github.com/thaleswillreis/multitenant-auth-api.git
cd multitenant-auth-api
cp .env.example .env
```

Edite o `.env` com suas próprias credenciais e gere uma chave de criptografia real para o MFA:

```bash
echo "MFA_ENCRYPTION_KEY=$(openssl rand -base64 32)" >> .env
```

### Rodando localmente (Maven + containers de infraestrutura)

Sobe só o Postgres e o Redis, e roda a aplicação diretamente na sua máquina:

```bash
docker compose up -d postgres redis
export $(grep -v '^#' .env | xargs)
./mvnw spring-boot:run
```

### Rodando via Docker Compose completo

Builda a imagem da aplicação e sobe toda a stack (banco, cache e API) de uma vez:

```bash
export $(grep -v '^#' .env | xargs)
docker compose up -d --build
```

### Rodando os testes

```bash
./mvnw clean verify
```

Isso executa a suíte completa (unitários via Surefire, integração via Failsafe com Testcontainers) e gera o relatório de cobertura combinado em `target/site/jacoco-merged/index.html`.

### Acessando a documentação da API

Com a aplicação no ar:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Especificação OpenAPI (JSON): `http://localhost:8080/v3/api-docs`
- Health check: `http://localhost:8080/health` ou `http://localhost:8080/actuator/health`
- Métricas Prometheus: `http://localhost:8080/actuator/prometheus`

## Problemas Enfrentados

Uma seleção dos desafios e investigações mais relevantes ao longo do desenvolvimento:

1. **`LazyInitializationException` em coleções do Hibernate carregadas fora de uma sessão ativa** — apareceu tanto no filtro de isolamento por tenant (Fase 1) quanto em testes de segurança criados depois (Fase 8). Resolvido garantindo que o código que acessa coleções `lazy` sempre rode dentro de uma transação ativa (`@Transactional`).
2. **Conflito entre múltiplas definições do mesmo filtro Hibernate (`@FilterDef`)** — ao adicionar uma nova entidade multi-tenant (`OAuthClient`), copiar o padrão de outra entidade duplicou a *definição* do filtro, que precisa existir uma única vez por unidade de persistência; entidades adicionais só precisam de `@Filter` para *usar* a definição já existente.
3. **Incompatibilidade de versão entre o springdoc-openapi e o Spring Framework** — uma versão recente do springdoc esperava encontrar uma classe (`LiteWebJarsResourceResolver`) que só existe a partir do Spring Framework 6.2, enquanto o Spring Boot 3.3.4 usado no projeto traz a versão 6.1.x. Diagnosticado analisando a árvore de dependências e o histórico de mudanças do Spring Framework, e resolvido fixando uma versão do springdoc testada especificamente contra essa linha do Boot.
4. **`@SpringBootTest` desativa observabilidade por padrão** — um teste de métricas falhava consistentemente só dentro da suíte de testes, nunca ao rodar a aplicação normalmente. A causa era um comportamento pouco divulgado do próprio Spring Boot Test, que desliga a exportação de métricas para evitar chamadas acidentais a um backend real durante testes — corrigido com `@AutoConfigureObservability` no teste específico que precisava validar isso.
5. **Rejeição silenciosa de um endpoint do Actuator** — o endpoint do Prometheus não aparecia, sem nenhum erro explícito. Isolado usando o relatório de avaliação de condições do próprio Spring Boot (flag `--debug`), que mostra exatamente qual pré-condição de auto-configuração não foi satisfeita.

## Resultados Obtidos

- **10 fases** e mais de **30 tarefas** concluídas de ponta a ponta.
- **98 testes automatizados** (unitários + integração), rodando a cada push via CI.
- **87% de cobertura de instruções** e **80% de cobertura de branches** (JaCoCo, relatório combinado unitários + integração).
- Conjunto completo de recursos de segurança: autenticação JWT, RBAC, OAuth2 Client Credentials, MFA via TOTP, rate limiting, proteção contra brute force, auditoria de eventos e varredura sistemática de vulnerabilidades comuns (adulteração de token, vazamento de dados, isolamento cross-tenant).
- Pipeline de CI/CD funcional, com build e suíte de testes completa validados automaticamente no GitHub Actions.
- Imagem Docker multi-stage e Docker Compose permitindo subir a stack inteira com um único comando.

## Melhorias Futuras / Próximos Passos

- Códigos de recuperação (recovery codes) para o MFA, cobrindo o cenário de perda do dispositivo autenticador.
- Endpoint de troca de senha para o usuário autenticado.
- Rate limiting por tenant, além do atual por IP.
- Tracing distribuído (ex: OpenTelemetry) complementando as métricas e logs já existentes.
- Testes de carga (ex: k6 ou Gatling) para validar o comportamento sob volume real de requisições.
- Deploy real e público, mantido no ar (hoje documentado apenas como guia, sem instância ativa — ver [`DEPLOY.md`](./DEPLOY.md)).
- Internacionalização das mensagens de erro (hoje apenas em português).

## Licença

Distribuído sob a licença MIT. Veja [`LICENSE`](./LICENSE) para mais detalhes.

## Contribuindo

Este é um projeto de portfólio, desenvolvido de forma solo com fins de estudo e demonstração de competências técnicas. Ainda assim, sugestões, correções e discussões técnicas são bem-vindas — sinta-se à vontade para abrir uma *issue* ou um *pull request*.