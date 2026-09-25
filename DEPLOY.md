# Guia de Deploy

Este documento descreve como implantar a **Multitenant Auth API** em um ambiente real, fora do seu computador de desenvolvimento. É um guia didático — não há, no momento, uma instância pública mantida no ar (veja o porquê dessa decisão na última seção).

## Visão geral

A aplicação já está pronta para deploy graças ao trabalho feito na Fase 10:

- Uma imagem Docker multi-stage (`Dockerfile`), enxuta e rodando como usuário não-root.
- Um `docker-compose.yml` que sobe a stack inteira (Postgres, Redis e a aplicação) com um único comando.
- Migrations gerenciadas automaticamente pelo Flyway a cada inicialização — nenhum passo manual de schema é necessário.

## Opção 1 — Deploy em um VPS com Docker Compose

O caminho mais direto, e o que mais se aproxima do que já rodamos localmente.

### Pré-requisitos no servidor
- Docker e Docker Compose instalados.
- Portas 8080 (aplicação), 5432 (Postgres) e 6379 (Redis) liberadas conforme sua necessidade — em produção, o recomendado é **não** expor 5432 e 6379 publicamente, deixando-as acessíveis só pela rede interna do Docker.

### Passos

1. Clone o repositório no servidor:
```bash
   git clone https://github.com/thaleswillreis/multitenant-auth-api.git
   cd multitenant-auth-api
```

2. Crie um `.env` de produção — **nunca reutilize os valores do `.env.example`**:
```bash
   cp .env.example .env
```
   Preencha com credenciais fortes e únicas, e gere uma chave de criptografia real para o MFA:
```bash
   echo "MFA_ENCRYPTION_KEY=$(openssl rand -base64 32)" >> .env
```

3. Ajuste o `docker-compose.yml` para não expor as portas do Postgres e do Redis publicamente (remova ou restrinja os blocos `ports` desses dois serviços, mantendo só o da aplicação).

4. Suba a stack:
```bash
   export $(grep -v '^#' .env | xargs)
   docker compose up -d --build
```

5. Configure um proxy reverso (Nginx, Caddy ou Traefik) na frente da aplicação, com TLS via Let's Encrypt — a aplicação em si não lida com HTTPS diretamente.

## Opção 2 — Plataformas gerenciadas (Render, Railway, Fly.io)

Essas plataformas simplificam bastante o processo: você aponta para o repositório (ou para o `Dockerfile`), configura as variáveis de ambiente pela interface delas, e a plataforma cuida do build e do TLS automaticamente.

Pontos de atenção comuns a todas elas:
- Adicione um serviço gerenciado de PostgreSQL e de Redis oferecido pela própria plataforma (a maioria oferece um tier gratuito ou de baixo custo) — não é necessário rodá-los você mesmo.
- Configure as mesmas variáveis de ambiente do `.env` na interface da plataforma: `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` (ou a URL de conexão completa que a plataforma fornecer), `REDIS_HOST`, `REDIS_PORT`, `MFA_ENCRYPTION_KEY`.
- O Flyway roda as migrations automaticamente no primeiro start — não é necessário nenhum passo manual de schema.

## Variáveis de ambiente necessárias

| Variável | Descrição |
|---|---|
| `POSTGRES_DB` | Nome do banco de dados |
| `POSTGRES_USER` | Usuário do PostgreSQL |
| `POSTGRES_PASSWORD` | Senha do PostgreSQL |
| `REDIS_HOST` | Host do Redis (`redis` dentro do Docker Compose) |
| `REDIS_PORT` | Porta do Redis |
| `MFA_ENCRYPTION_KEY` | Chave AES-256 (base64) para criptografar segredos de MFA — gere com `openssl rand -base64 32` |

## Por que não há uma instância pública mantida no ar

Duas limitações reais tornam uma instância "sempre viva" pouco representativa do valor do projeto:

1. **As chaves RSA do JWT são geradas em memória a cada inicialização** (uma decisão deliberada da Fase 3, documentada no README) — qualquer reinício da aplicação invalida todas as sessões ativas silenciosamente. Aceitável para demonstração pontual, mas não para uma instância pública de longo prazo.
2. **Custo e manutenção contínua** — mesmo em camadas gratuitas de hospedagem, manter algo no ar indefinidamente exige atenção a expiração de recursos, patches de segurança e monitoramento — esforço que não agrega ao propósito principal do projeto, que é demonstrar competência técnica através do próprio código, testes e documentação.

Se você quiser rodar uma instância própria para avaliação, os passos deste guia são suficientes para isso.