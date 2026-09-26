<div align="center">

# 🧾 Cuadremos

**Split shared expenses. Keep the accounts square.**

*"¡Cuadremos!" — Colombian for "let's settle up."*

[![CI](https://github.com/cris-sh/cuadremos/actions/workflows/ci.yaml/badge.svg)](https://github.com/cris-sh/cuadremos/actions/workflows/ci.yaml)
![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18-4169E1?logo=postgresql&logoColor=white)
![Flyway](https://img.shields.io/badge/Flyway-migrations-CC0200?logo=flyway&logoColor=white)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**English** · [Español](README.es.md)
</div>

---

## ✨ What is Cuadremos?

Cuadremos is a REST API for splitting shared expenses with friends, family, roommates or travel buddies. Create a group, log who paid for what, and let the API work out who owes whom — and the fewest payments needed to settle up.

If you've used apps like Splitwise or Tricount, the idea will feel familiar.

Cuadremos started as a personal project to learn backend engineering by building something real, so it's built with layered architecture, security, testing, database migrations and CI from day one.

## 🚦 Status

| Feature                                          | Status         |
|--------------------------------------------------|----------------|
| 🔐 Registration & JWT login                      | ✅ Done         |
| 👥 Groups with owner, admins and members         | ✅ Done         |
| 🛡️ Group-level permissions                      | 🚧 In progress |
| 💰 Expenses (equal, exact adn percentage splits) | 🗺️ Planned    |
| ⚖️ Balances & "who owes whom"                    | 🗺️ Planned    |
| 💸 Settle-up suggestions with debt minimization  | 🗺️ Planned    |

## 🧰 Tech stack
| Layer | Tools                                                            |
|---|------------------------------------------------------------------|
| Language & Framework | Java 21 · Spring Boot 4.1 · Spring Web MVC                       |
| Security | Spring Security · OAuth2 Resource Server (JWT, HS256) · Argon2id |
| Persistence | Spring Data JPA                                                  | Hibernate | PostgreSQL · Flyway |
| Testing | JUnit 5 · Mockito · AssertJ · MockMvc · Spring Security Test     |
| Tooling | Maven · Lombok · GitHub Actions · Dependabot |

## 🏗️ Architecture

The code is split into layers. Each one only talks to the layer below it.

```mermaid
flowchart LR
    C([Client]) -- "HTTP + Bearer JWT" --> A
    A["<b>api</b><br/>controllers<br/>error handling"] --> S
    S["<b>application</b><br/>services · DTOs"] --> D
    S --> I
    D["<b>domain</b><br/>entities · business rules"]
    I["<b>infrastructure</b><br/>repositories · security"] --> DB[(PostgreSQL)]
```

```
src/main/java/pw/cris/cuadremos
├── api/              → REST controllers and the global exception handler
├── application/      → services (use cases) and request/response DTOs
├── domain/           → entities, roles and domain exceptions
└── infrastructure/   → JPA repositories, JWT and Spring Security config
```

## 🗃️ Data model

```mermaid
erDiagram
    USERS ||--o{ GROUP_MEMBERS : joins
    GROUPS ||--o{ GROUP_MEMBERS : has
    USERS ||--o{ GROUPS : owns
    GROUPS ||--o{ EXPENSES : contains
    USERS ||--o{ EXPENSES : pays
    EXPENSES ||--o{ EXPENSE_SHARES : "split into"
    USERS ||--o{ EXPENSE_SHARES : owes
    GROUPS ||--o{ SETTLEMENTS : records

    USERS {
        uuid id PK
        varchar username UK
        varchar email UK
        varchar password "Argon2id hash"
    }
    GROUPS {
        uuid id PK
        varchar name
        uuid owner_id FK
    }
    GROUP_MEMBERS {
        uuid id PK
        uuid group_id FK
        uuid user_id FK
        varchar role "ADMIN | MEMBER"
    }
    EXPENSES {
        uuid id PK
        decimal amount
        uuid paid_by_user_id FK
    }
```

## 🔌 API

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | — | Create an account |
| `POST` | `/api/auth/login` | — | Get a JWT access token |
| `POST` | `/api/groups` | 🔑 | Create a group (you become its owner) |
| `GET` | `/api/groups/{groupId}` | 🔑 | Get a group and its members |
| `POST` | `/api/groups/{groupId}/members` | 🔑 | Add a member by username |

<details>
<summary><b>Try it with curl</b></summary>

```bash
# 1. Register
curl -X POST localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"cris","name":"Cristian","email":"cris@example.com","password":"supersecret123"}'

# 2. Log in and grab the token
curl -X POST localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"cris","password":"supersecret123"}'
# → {"accessToken":"eyJ...","tokenType":"Bearer","expiresIn":3600}

# 3. Create a group
curl -X POST localhost:8080/api/groups \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Trip to Cúcuta"}'
```

Every member comes back with a `role` and an `owner` flag, so a client can show the owner as an admin with a 👑:

```json
{
  "id": "5f0c…",
  "name": "Trip to Cúcuta",
  "members": [
    { "id": "9a1e…", "username": "cris", "name": "Cristian", "role": "ADMIN", "owner": true }
  ],
  "createdAt": "2026-09-25T18:30:00Z"
}
```

</details>

## 🚀 Getting started

**Prerequisites:** Java 21 and a PostgreSQL 13+ database (local, Docker or a hosted one like Neon).

**1. Clone**

```bash
git clone https://github.com/cris-sh/cuadremos.git
cd cuadremos
```

**2. Create a `.env` file** in the project root (it is git-ignored):

```properties
DATABASE_URL=jdbc:postgresql://localhost:5432/cuadremos
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=postgres
# At least 32 bytes. Generate one with: openssl rand -base64 64
JWT_SECRET=change-me-to-a-long-random-secret
```

**3. Run**

```bash
./mvnw spring-boot:run
```

Flyway creates the schema on first start, and the API listens on `http://localhost:8080`.

**4. Test**

```bash
./mvnw test
```

> [!NOTE]
> `./mvnw test` includes a full application test that connects to the database in your `.env` and applies pending migrations. The unit tests alone need no database.

## 🧠 Design decisions

- **Argon2id password hashing.** Memory-hard, the current OWASP recommendation. Passwords never leave the service in plain text.
- **Stateless JWT auth.** Tokens are signed with HS256 and carry the user id as their subject, so no session is stored server-side. Tokens expire after one hour.
- **Normalised identities.** Usernames and emails are lowercased before they are checked and stored, so `Cris` and `cris` can never become two accounts.
- **Membership is an entity, ownership is a column.** `group_members` carries each member's role, while `groups.owner_id` guarantees exactly one owner per group and makes ownership transfers a single update.
- **Entities compare by id.** `User` implements `equals`/`hashCode` by database id, so sets of members behave correctly across transactions.
- **The schema is code.** Every change is a versioned Flyway migration, and Hibernate only *validates* the schema; it never alters it.
- **Deliberately vague auth errors.** A failed login never reveals whether the username exists.

## 🗺️ Roadmap

- [x] **Phase 1 — Auth:** registration, JWT login, locked-down endpoints
- [ ] **Phase 1 — Groups:** roles and ownership ✅, permissions, removing members, promoting admins, ownership transfer, archiving
- [ ] **Phase 2 — Expenses:** equal, exact and percentage splits; shares always add up to the cent
- [ ] **Phase 3 — Balances:** net balance per member and greedy debt minimization
- [ ] **Phase 4 — Production:** Testcontainers, OpenAPI docs, Docker Compose, structured logging
- [ ] **Phase 5 — Extras:** invite links, categories, recurring expenses, multi-currency

## 📄 License

Released under the [MIT License](LICENSE).

<div align="center">

Made with ☕ in Colombia by [cris-sh](https://github.com/cris-sh)

</div>