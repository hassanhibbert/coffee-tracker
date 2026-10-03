# Coffee Tracker API (server)

> Part of the [Coffee Tracker monorepo](../README.md). The React client lives in [`client/`](../client).

A REST API for logging coffee brews (AeroPress, Vietnamese phin, pour-over, espresso…) and the
parameters behind them: beans, equipment, grind size, coffee-to-water ratio, temperature, time
and how good it tasted.

Built with **Java 21 · Spring Boot 4.1 · PostgreSQL · Flyway · Spring Security (JWT) · Testcontainers**.
No Lombok, on purpose: every constructor, getter and query is visible.

## Features

- **Full CRUD** for beans, equipment and brews, layered `Controller → Service → Repository`
- **JWT authentication** (register/login); every user sees only their own data
- **Validation** at two levels: Bean Validation on requests, and a domain rule that grind
  size must suit the brew method (no coarse grind for espresso)
- **Consistent errors** as [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) Problem Details
- **Filtering + pagination** on brews (`?method=PHIN&minRating=4&sort=rating,desc`)
- **Stats**: totals, per-method averages, top-rated beans (JPQL aggregate projections)
- **Schema migrations** with Flyway, with `CHECK` constraints and indexed foreign keys
- **OpenAPI docs** with Swagger UI at `/swagger-ui.html`
- **Tests** at every level: unit, web slice, repository against real Postgres, end to end
- **Docker image** and a one-click **Render** blueprint; CI on GitHub Actions

## API

All endpoints except `/api/v1/auth/**` require `Authorization: Bearer <token>`.

| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/auth/register` | Create an account, returns a token |
| POST | `/api/v1/auth/login` | Returns a token (valid 1 hour) |
| GET / POST | `/api/v1/beans` | List / create beans |
| GET / PUT / DELETE | `/api/v1/beans/{id}` | Read / replace / delete a bean |
| GET / POST | `/api/v1/equipment` | List / create equipment |
| GET / PUT / DELETE | `/api/v1/equipment/{id}` | Read / replace / delete equipment |
| GET | `/api/v1/brews` | Search brews (filters + pagination, below) |
| POST | `/api/v1/brews` | Log a brew |
| GET / PUT / DELETE | `/api/v1/brews/{id}` | Read / replace / delete a brew |
| GET | `/api/v1/stats` | Brew statistics |
| GET | `/actuator/health` | Health check (public) |

**Brew search parameters:** `beanId`, `equipmentId`, `method`, `grindSize`, `minRating`,
`from`, `to` (ISO-8601 instants), `page`, `size` (max 100), `sort` (`brewedAt`, `rating`,
`coffeeGrams`, `waterGrams`, `brewTimeSeconds`). The default is newest first.

### Example

```bash
TOKEN=$(curl -s -X POST localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"hassan","password":"correct-horse"}' | jq -r .accessToken)

curl -s -X POST localhost:8080/api/v1/brews \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"beanId":1,"equipmentId":1,"grindSize":"FINE","coffeeGrams":20,"waterGrams":100,
       "waterTempC":96,"brewTimeSeconds":300,"rating":5,"notes":"Classic phin"}'
```

```json
{
  "id": 1,
  "bean": { "id": 1, "name": "Dak Lak Robusta" },
  "equipment": { "id": 1, "name": "Phin", "method": "PHIN" },
  "grindSize": "FINE",
  "coffeeGrams": 20.0,
  "waterGrams": 100.0,
  "ratio": "1:5.0",
  "waterTempC": 96,
  "brewTimeSeconds": 300,
  "rating": 5,
  "notes": "Classic phin",
  "brewedAt": "2026-10-03T12:00:00Z"
}
```

Errors share one shape:

```json
{
  "type": "about:blank",
  "title": "Brew rule violated",
  "status": 422,
  "detail": "Grind size COARSE does not suit ESPRESSO (expected EXTRA_FINE to FINE)",
  "instance": "/api/v1/brews"
}
```

## Running locally

Requirements: JDK 21 and PostgreSQL (e.g. [Postgres.app](https://postgresapp.com)).
Docker is optional; it is only needed to run the Testcontainers tests.

```bash
cd server
createdb coffee_tracker
export JWT_SECRET=$(openssl rand -base64 48)   # required: the app refuses to start without it
./mvnw spring-boot:run
```

Flyway creates the schema on startup. Open http://localhost:8080/swagger-ui.html, call
`/auth/register`, then click **Authorize** and paste the token.

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL` | built from the parts below | Full JDBC URL (overrides the parts) |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `5432` / `coffee_tracker` | Database location |
| `DB_USERNAME` / `DB_PASSWORD` | `h2code` / *(empty)* | Database credentials |
| `JWT_SECRET` | **none** | HMAC key for signing tokens, 32+ bytes |
| `JWT_TTL` | `PT1H` | Token lifetime (ISO-8601 duration) |
| `DB_POOL_SIZE` | `5` | Hikari connection pool size |
| `PORT` | `8080` | HTTP port |
| `CORS_ALLOWED_ORIGINS` | *(none)* | Comma-separated browser origins allowed to call the API |

## Tests

```bash
cd server && ./mvnw test
```

| Level | Tools | What it covers |
|---|---|---|
| Unit | JUnit 5, Mockito | Grind rule, ratio maths, ownership checks, sort whitelist, login behaviour |
| Web slice | `@WebMvcTest` + real security config | Status codes, validation errors, JWT → user id |
| Repository | `@DataJpaTest` + Testcontainers Postgres | Specifications, JPQL stats, DB `CHECK` constraints |
| End to end | `@SpringBootTest` + Testcontainers | Register → log brew → users isolated; forged tokens rejected |

Repository and end-to-end tests run against a real PostgreSQL 18 container, not H2, so they
exercise the same SQL dialect, constraints and Flyway migrations as production. Without Docker
they are skipped rather than failed; CI always runs them.

## Deployment (Render)

The repo includes a [Render Blueprint](../render.yaml). In Render, choose **New → Blueprint** and
pick this repository. Render provisions a free Postgres instance and builds the
[Dockerfile](Dockerfile), wires the database credentials into the service, and generates
`JWT_SECRET`.

> Free Render services sleep when idle, so the first request after a while takes ~30–60 s
> while the JVM starts. Free databases expire after 30 days.

## Design notes

- **Owner-scoped queries.** Repositories only expose `findByIdAndOwnerId`-style lookups. A
  request for another user's resource gets `404`, not `403`, so ids can't be probed.
- **DTOs at the boundary.** Entities never reach Jackson. Responses are records built inside
  the service transaction, and `spring.jpa.open-in-view` is off, so lazy loading can't fire
  hidden queries during serialization.
- **No N+1.** Brew queries fetch bean and equipment with an `@EntityGraph`: a page of brews
  is one SQL statement.
- **Two layers of constraints.** Bean Validation gives readable 400s; Postgres `CHECK`s are
  the backstop if anything bypasses the API.
- **Indexed foreign keys.** Postgres doesn't index FKs automatically; the migrations do
  (`brews.bean_id`, `brews.equipment_id`, `(owner_id, brewed_at desc)`).
- **Stateless JWT.** HS256 tokens with a short TTL, verified by Spring Security's resource
  server. BCrypt passwords; login takes the same time for unknown users and wrong passwords,
  so it can't be used to discover usernames.

## Project structure

```
src/main/java/com/hassanhibbert/coffee_tracker
├── auth/         register/login endpoints and service
├── config/       OpenAPI configuration
├── controller/   REST controllers (thin: HTTP in, DTO out)
├── dto/          request/response records, filters, paging, stats
├── entity/       JPA entities and enums (BrewMethod knows its valid grind range)
├── exception/    domain exceptions + global ProblemDetail handler
├── repository/   Spring Data repositories and query specifications
├── security/     security filter chain, JWT encoding, @CurrentUserId
└── service/      business logic and transactions
src/main/resources/db/migration   Flyway SQL migrations
```
