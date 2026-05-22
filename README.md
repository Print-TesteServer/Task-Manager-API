# Task Manager API

[![CI](https://github.com/Print-TesteServer/Teste-Delta/actions/workflows/ci.yml/badge.svg)](https://github.com/Print-TesteServer/Teste-Delta/actions/workflows/ci.yml)

REST API para gerenciamento de tarefas com autenticação JWT, desenvolvida em **Kotlin + Spring Boot**. Teste técnico Delta.

---

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Kotlin 1.9 |
| Framework | Spring Boot 3.2 |
| Banco de Dados | SQLite (via Hibernate Community Dialects) |
| Autenticação | JWT (jjwt 0.12) |
| Documentação | Swagger / OpenAPI 3 (springdoc) |
| Build | Gradle (Kotlin DSL) |
| Containerização | Docker + Docker Compose |

---

## Como executar

### Opção 1 — Docker (recomendado, sem dependências locais)

```bash
git clone https://github.com/Print-TesteServer/Teste-Delta.git
cd Teste-Delta

docker compose up --build
```

Quando a aplicação subir, o terminal exibirá um bloco com as URLs (clicáveis no VS Code, Windows Terminal e Docker Desktop).

### Onde abrir no navegador

Este projeto é uma **API REST** (JSON). A única interface visual para testar no navegador é o **Swagger UI**:

**http://localhost:8080/swagger-ui/index.html**

| URL | Uso |
|-----|-----|
| [Swagger UI](http://localhost:8080/swagger-ui/index.html) | Testar cadastro, login e tarefas no navegador |
| [OpenAPI JSON](http://localhost:8080/api-docs) | Especificação da API |
| `http://localhost:8080/` | Não há página inicial (pode retornar 401) |
| `http://localhost:8080/api/tasks` | Requer JWT — no navegador sem token retorna **401** (JSON) |

**Fluxo no Swagger:** `POST /api/auth/register` ou `/login` → copie o `token` → botão **Authorize** → `Bearer <token>` → teste os endpoints em **Tasks**.

> O banco SQLite é persistido em `./data/taskmanager.db` no host.

---

### Opção 2 — Gradle local (requer JDK 17+)

```bash
git clone https://github.com/Print-TesteServer/Teste-Delta.git
cd Teste-Delta

# Linux / macOS
./gradlew bootRun

# Windows
gradlew.bat bootRun
```

Ao iniciar, as mesmas URLs do Swagger aparecem no terminal. Requer **JDK 17** no `JAVA_HOME` (Gradle 8.7 não roda com Java 25).

#### Configurar `JAVA_HOME` no Windows (PowerShell)

```powershell
# Verifique onde o JDK está instalado (ex.: Temurin 17)
where.exe java

# Defina para a sessão atual (sem barra no final do caminho)
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.13.11-hotspot"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

# Confirme
java -version
.\gradlew.bat test
```

Para tornar permanente: **Configurações → Sistema → Sobre → Configurações avançadas do sistema → Variáveis de ambiente** → crie ou edite `JAVA_HOME` apontando para a pasta do JDK (não para `bin`).

#### Variáveis de ambiente locais (opcional)

Copie `.env.example` para `.env` e ajuste se necessário. O Spring Boot lê `JWT_SECRET`, `JWT_EXPIRATION` e `SPRING_DATASOURCE_URL` automaticamente.

---

## Teste manual rápido (E2E)

Com a API rodando em `http://localhost:8080`:

```bash
# 1. Cadastro (guarde o token da resposta)
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Teste","email":"teste@email.com","password":"senha123"}'

# 2. Login (alternativa)
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"teste@email.com","password":"senha123"}'

# 3. Criar tarefa (substitua TOKEN)
curl -s -X POST http://localhost:8080/api/tasks \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"Minha tarefa","status":"PENDING"}'

# 4. Listar tarefas do usuário autenticado
curl -s http://localhost:8080/api/tasks -H "Authorization: Bearer TOKEN"
```

Fluxo esperado: cadastro/login retorna **201/200** com `token`; criar tarefa retorna **201**; sem token, `GET /api/tasks` retorna **401**.

---

## Documentação interativa (Swagger UI)

Link direto: **http://localhost:8080/swagger-ui/index.html**

1. Execute a aplicação (Docker ou `gradlew bootRun`).
2. Abra o link acima (ou clique na URL impressa no terminal).
3. Em **Authentication**, faça register/login e copie o `token`.
4. Clique em **Authorize** e informe `Bearer <seu-token>`.
5. Use os endpoints em **Tasks**.

---

## Endpoints

### Autenticação (`/api/auth`)

| Método | Rota | Descrição | Auth |
|---|---|---|---|
| `POST` | `/api/auth/register` | Cadastra novo usuário | ❌ |
| `POST` | `/api/auth/login` | Login e retorna token JWT | ❌ |

#### Exemplo — Cadastro

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name": "Matheus", "email": "matheus@email.com", "password": "senha123"}'
```

**Resposta 201:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "userId": 1,
  "name": "Matheus",
  "email": "matheus@email.com"
}
```

#### Exemplo — Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "matheus@email.com", "password": "senha123"}'
```

---

### Tarefas (`/api/tasks`) — requer JWT

Inclua o header `Authorization: Bearer <token>` em todas as requisições abaixo.

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/tasks` | Cria nova tarefa |
| `GET` | `/api/tasks` | Lista todas as tarefas do usuário |
| `GET` | `/api/tasks?status=DONE` | Filtra tarefas por status |
| `GET` | `/api/tasks/{id}` | Busca tarefa por ID |
| `PUT` | `/api/tasks/{id}` | Atualiza título, descrição e/ou status |
| `PATCH` | `/api/tasks/{id}/done` | Marca tarefa como DONE |
| `DELETE` | `/api/tasks/{id}` | Remove tarefa |

**Status disponíveis:** `PENDING` | `IN_PROGRESS` | `DONE`

#### Exemplos

```bash
TOKEN="eyJhbGciOiJIUzI1NiJ9..."

# Criar tarefa
curl -X POST http://localhost:8080/api/tasks \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title": "Implementar autenticação", "description": "JWT com Spring Security", "status": "IN_PROGRESS"}'

# Listar tarefas
curl http://localhost:8080/api/tasks \
  -H "Authorization: Bearer $TOKEN"

# Marcar como concluída
curl -X PATCH http://localhost:8080/api/tasks/1/done \
  -H "Authorization: Bearer $TOKEN"

# Deletar tarefa
curl -X DELETE http://localhost:8080/api/tasks/1 \
  -H "Authorization: Bearer $TOKEN"
```

---

## Estrutura do projeto

```
src/main/kotlin/com/delta/taskmanager/
├── TaskManagerApplication.kt       # Entry point
├── config/
│   ├── SecurityConfig.kt           # Spring Security + JWT filter
│   └── OpenApiConfig.kt            # Swagger / OpenAPI config
├── controller/
│   ├── AuthController.kt           # POST /api/auth/**
│   └── TaskController.kt           # CRUD /api/tasks/**
├── dto/
│   ├── auth/                       # RegisterRequest, LoginRequest, AuthResponse
│   └── task/                       # CreateTaskRequest, UpdateTaskRequest, TaskResponse
├── entity/
│   ├── User.kt                     # Entidade JPA — usuários
│   └── Task.kt                     # Entidade JPA — tarefas (+ enum TaskStatus)
├── exception/
│   ├── Exceptions.kt               # Exceções de domínio
│   └── GlobalExceptionHandler.kt   # @RestControllerAdvice com respostas padronizadas
├── repository/
│   ├── UserRepository.kt           # Spring Data JPA
│   └── TaskRepository.kt
├── security/
│   ├── JwtService.kt               # Geração e validação de tokens
│   ├── JwtAuthFilter.kt            # OncePerRequestFilter
│   └── UserDetailsServiceImpl.kt   # Carrega usuário por e-mail
└── service/
    ├── AuthService.kt              # Lógica de cadastro e login
    └── TaskService.kt              # CRUD de tarefas com controle de propriedade
```

---

## Testes

```bash
# Linux / macOS
./gradlew test

# Windows
gradlew.bat test

# Docker
docker run --rm -v "${PWD}:/app" -w /app gradle:8.7-jdk17 gradle test --no-daemon
```

Cobertura:
- **AuthServiceTest** — registro, login, validação de senha, e-mail duplicado
- **TaskServiceTest** — CRUD completo, filtragem por status, controle de acesso entre usuários
- **AuthControllerTest** — validação de payload, status HTTP, tratamento de erros
- **TaskControllerTest** — endpoints protegidos, 401/403/404 em cenários de erro
- **TaskManagerIntegrationTest** — fluxo completo com JWT real (H2), isolamento entre usuários (403), e-mail duplicado (409)

O pipeline **GitHub Actions** (`.github/workflows/ci.yml`) executa `./gradlew test` em cada push na branch `main`.

---

## Segurança

- Senhas armazenadas com **BCrypt**
- Tokens JWT assinados com **HMAC-SHA256**
- Cada endpoint de tarefa verifica se o `user_id` da tarefa coincide com o usuário autenticado — acesso negado com **403** caso contrário
- Sessão **stateless** (sem cookies, sem estado no servidor)

---

## Variáveis de ambiente

| Variável | Padrão (dev) | Descrição |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:sqlite:./taskmanager.db` | URL JDBC do banco |
| `JWT_SECRET` | valor em `application.yml` | Segredo **Base64** (256 bits) para assinar JWT |
| `JWT_EXPIRATION` | `86400000` | Expiração do token em ms (24h) |
| `SERVER_PORT` | `8080` | Porta HTTP |

No Docker Compose, `JWT_SECRET` e `SPRING_DATASOURCE_URL` já são injetados. Para desenvolvimento local, use `.env.example` como referência.

> ⚠️ Em produção, gere um `JWT_SECRET` aleatório (Base64, 256 bits) e injete via variável de ambiente — nunca commite segredos reais no repositório.
