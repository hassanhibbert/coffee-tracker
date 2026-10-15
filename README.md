# Coffee Tracker

A full-stack app for logging coffee brews (AeroPress, Vietnamese phin, pour-over,
espresso…) and the parameters behind them: beans, equipment, grind size, coffee-to-water
ratio, temperature, time, and how good the cup was.

| | Stack | Docs |
|---|---|---|
| **[`server/`](server)** | Java 21 · Spring Boot 4.1 · PostgreSQL · Flyway · Spring Security (JWT) · Testcontainers | [server/README.md](server/README.md) |
| **[`client/`](client)** | React 19 · TypeScript · Vite · TanStack Query · React Router · Tailwind CSS · Vitest | [client/README.md](client/README.md) |

## Highlights

- **Typed end to end.** The client's TypeScript types are generated from the server's
  OpenAPI spec, so a change to a Java DTO surfaces as a compile error in React.
- **Secure by default.** JWT auth, per-user data isolation (IDOR-safe), BCrypt, CORS
  allow-list, and a strict Content-Security-Policy on the client.
- **Validated twice, explained once.** Bean Validation and Postgres `CHECK` constraints on
  the server; the client renders the server's RFC 9457 errors next to the right fields.
- **Tested at every layer.** JUnit/Mockito units, Spring slice tests, Testcontainers
  against real Postgres, and React Testing Library + MSW on the client.
- **Deployable.** Dockerfile + Render Blueprint (API, database), the client on DreamHost, and GitHub
  Actions CI for both apps.

## Quick start

Requirements: JDK 21, Node 22+, PostgreSQL. Docker is optional (for Testcontainers tests).

```bash
# 1. API on :8080
cd server
createdb coffee_tracker
export JWT_SECRET=$(openssl rand -base64 48)
./mvnw spring-boot:run

# 2. Client on :5173 (in a second terminal)
cd client
npm install
npm run dev
```

Open http://localhost:5173, create an account, add a bean and some equipment, and log a brew.
API docs: http://localhost:8080/swagger-ui.html.

## Repository layout

```
.
├── server/                 Spring Boot API (Maven), Dockerfile
├── client/                 React app (Vite)
├── render.yaml             Render Blueprint: Postgres + API
└── .github/workflows/      CI: server (tests + Docker build) and client (lint, test, build)
```
