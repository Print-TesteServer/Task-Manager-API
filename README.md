# Task Manager API

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
git clone https://github.com/seu-usuario/task-manager.git
cd task-manager

docker compose up --build
```

A API estará disponível em `http://localhost:8080`.

> O banco SQLite é persistido em `./data/taskmanager.db` no host.

---

### Opção 2 — Gradle local (requer JDK 17+)

```bash
git clone https://github.com/seu-usuario/task-manager.git
cd task-manager

# Linux / macOS
./gradlew bootRun

# Windows
gradlew.bat bootRun
```

> Se não tiver o Gradle Wrapper no projeto, instale o Gradle 8+ e rode `gradle bootRun`.

---

## Documentação interativa (Swagger UI)

Após subir a aplicação, acesse:

```
http://localhost:8080/swagger-ui/index.html
```

Clique em **Authorize** e cole o token JWT obtido no login para testar os endpoints protegidos.

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
./gradlew test
```

Cobertura:
- **AuthServiceTest** — registro, login, validação de senha, e-mail duplicado
- **TaskServiceTest** — CRUD completo, filtragem por status, controle de acesso entre usuários
- **AuthControllerTest** — validação de payload, status HTTP, tratamento de erros
- **TaskControllerTest** — endpoints protegidos, 401/403/404 em cenários de erro

---

## Segurança

- Senhas armazenadas com **BCrypt**
- Tokens JWT assinados com **HMAC-SHA256**
- Cada endpoint de tarefa verifica se o `user_id` da tarefa coincide com o usuário autenticado — acesso negado com **403** caso contrário
- Sessão **stateless** (sem cookies, sem estado no servidor)

---

## Variáveis de ambiente (produção)

| Variável | Padrão | Descrição |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:sqlite:./taskmanager.db` | URL do banco |
| `JWT_SECRET` | *(ver application.yml)* | Segredo Base64 de 256 bits |
| `JWT_EXPIRATION` | `86400000` | Expiração do token em ms (24h) |
| `SERVER_PORT` | `8080` | Porta HTTP |

> ⚠️ Em produção, troque o `JWT_SECRET` por um valor gerado aleatoriamente e nunca o commite no repositório.
