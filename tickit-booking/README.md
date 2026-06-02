# TickIt — Containerized Ticket Booking Backend (Maven)

A Spring Boot 3.5 + Thymeleaf + PostgreSQL 16 ticket-booking application, built to
the project's 12-Factor IaC conventions and orchestrated with **Podman Desktop** on
Windows 11. It implements three user stories — browse events, book tickets (with
pessimistic locking to prevent overbooking), and cancel a booking (releasing seats).

Both a server-rendered **Thymeleaf UI** and a **REST API** (`/api/v1/...`) are exposed,
sharing one Controller → Service → Repository stack.

> Build tool: **Maven** (wrapper included — no local Maven install needed).

---

## Tech stack
| | |
|---|---|
| Java | 21 |
| Framework | Spring Boot 3.5.x (Web, Thymeleaf, Data JPA, Validation, Actuator) |
| Database | PostgreSQL 16 (Alpine) |
| Migrations | Flyway (`src/main/resources/db/migration`) |
| Build | Maven (`mvnw` wrapper) |
| Containers | Podman Desktop / OCI, single `docker-compose.yml` |
| Tests | JUnit 5 + Mockito (unit) and Testcontainers (integration) |

---

## Lombok setup (important if your IDE shows errors)

This project uses Lombok (`@Getter`, `@Builder`, `@RequiredArgsConstructor`, `@Slf4j`, …).
The Maven build is already configured to run the Lombok annotation processor, so
`.\mvnw.cmd clean package` works from the command line with no extra steps.

Your **IDE** needs Lombok enabled separately, or it will report errors like
*"log cannot be resolved"*, *"builder() is undefined"*, or *"blank final field … may
not have been initialized"* even though the Maven build succeeds:

- **Eclipse / Spring Tool Suite (STS):** Lombok must be installed into the IDE itself.
  Locate the lombok jar (after a build it's at
  `%USERPROFILE%\.m2\repository\org\projectlombok\lombok\<version>\lombok-<version>.jar`),
  then run `java -jar lombok-<version>.jar`. The installer detects your Eclipse/STS
  installation — click *Install / Update*, then **restart the IDE** and do
  *Project → Clean*. (Equivalent to patching `eclipse.ini` with the lombok javaagent.)
- **IntelliJ IDEA:** install the *Lombok* plugin (bundled in recent versions) and enable
  *Settings → Build, Execution, Deployment → Compiler → Annotation Processors →
  Enable annotation processing*.
- **VS Code:** install the *Lombok Annotations Support* extension (or it ships with the
  Extension Pack for Java), then reload the window.

To confirm it's purely an IDE issue, run `.\mvnw.cmd clean compile` — if that succeeds,
the code is fine and only the editor needs Lombok enabled.

---

## Prerequisites
- **Podman Desktop** running on Windows 11 (Docker Compose syntax is supported by `podman compose`).
- JDK 21 only needed if you want to run the app outside a container (Option B).
- No local Maven required — `mvnw` / `mvnw.cmd` downloads the right version on first run.

---

## Quick start (Option A — full stack in Podman)

```powershell
# 1. Provide configuration/secrets (Factor III). Never commit the resulting .env.
copy .env.example .env

# 2. Build and start app + database together.
podman compose up --build
```

- App UI:        http://localhost:8080/
- REST events:   http://localhost:8080/api/v1/events
- Health:        http://localhost:8080/actuator/health

The database container comes up first; the app waits for its `pg_isready`
health check before starting (Factor IX). Flyway then creates the schema and
seeds demo events automatically at boot (Factor XII).

Stop with `Ctrl+C`, then `podman compose down` (add `-v` to also drop the data volume).

---

## Quick start (Option B — DB in Podman, app on host)

Useful for fast iteration in your IDE.

```powershell
# Start only PostgreSQL in a container, persisting data to a named volume.
podman run -d --name tickit-db `
  -e POSTGRES_DB=tickit -e POSTGRES_USER=tickit -e POSTGRES_PASSWORD=tickit `
  -p 5432:5432 `
  -v tickit-pgdata:/var/lib/postgresql/data `
  postgres:16-alpine

# Run the app (uses the localhost defaults in application.yml).
.\mvnw.cmd spring-boot:run
```

Connection string pattern: `jdbc:postgresql://localhost:5432/tickit`

---

## Building and running the tests

```powershell
# Build the bootable jar (skips tests)
.\mvnw.cmd clean package -DskipTests

# Run all tests
.\mvnw.cmd test

# Run only the fast unit tests (no container engine needed)
.\mvnw.cmd test -Dtest=BookingServiceTest
```

- **`BookingServiceTest`** — pure unit tests (Mockito), no container needed. Cover
  seat decrement, overbooking rejection, and seat release on cancel.
- **`BookingIntegrationTest`** — full-stack tests against a real PostgreSQL 16
  container via Testcontainers. **Requires Podman/Docker running.** Includes a
  10-thread concurrency test proving the pessimistic write lock prevents overbooking.

> Testcontainers talks to a container engine over a socket. With Podman Desktop,
> enable the Docker-compatible socket (Settings → "Docker compatibility"), or set
> `DOCKER_HOST` to the Podman machine socket so Testcontainers can find it.

The bootable jar is produced at `target/tickit-booking-0.0.1-SNAPSHOT.jar` and can be
run directly with `java -jar target/tickit-booking-0.0.1-SNAPSHOT.jar`.

---

## API reference

| Method | Path | Story | Notes |
|---|---|---|---|
| `GET` | `/api/v1/events` | US1 | Upcoming, non-sold-out events |
| `POST` | `/api/v1/bookings` | US2 | Body: `{eventId, customerName, customerEmail, quantity}` |
| `DELETE` | `/api/v1/bookings/{id}` | US3 | Releases seats |
| `GET` | `/api/v1/bookings?email=` | — | Bookings for a customer |

Example:
```powershell
curl -X POST http://localhost:8080/api/v1/bookings `
  -H "Content-Type: application/json" `
  -d '{"eventId":1,"customerName":"Jane","customerEmail":"jane@example.com","quantity":2}'
```

---

## How the 12-Factor conventions are applied

| Factor | Where |
|---|---|
| I Codebase | One `docker-compose.yml`; no env-specific copies |
| II Dependencies | Pinned tags (`postgres:16-alpine`, `maven:3.9.9-eclipse-temurin-21`, `eclipse-temurin:21-jre-jammy`) |
| III Config | All secrets in `.env` (git-ignored); read via env vars |
| IV Backing services | DB attached via `SPRING_DATASOURCE_URL` |
| V Build/Release/Run | Multi-stage `Dockerfile` |
| VI Processes | Stateless app; runs as non-root; no local session files |
| VII Port binding | `8080:8080` |
| VIII Concurrency | Stateless process scales horizontally (`podman compose up --scale app=N`*) |
| IX Disposability | `pg_isready` + actuator health checks; graceful shutdown |
| X Dev/prod parity | Same image & PostgreSQL 16 in tests (Testcontainers) and runtime |
| XI Logs | Logback to stdout/stderr only |
| XII Admin processes | Flyway migrations run as an automated boot hook |

\* Remove the fixed `container_name` / host port mapping on `app` before scaling.

---

## Project layout
```
pom.xml              Maven build
mvnw / mvnw.cmd      Maven wrapper (.mvn/wrapper/maven-wrapper.properties)
src/main/java/com/stc/tickit
├── domain/        JPA entities (Event, Booking, AppUser, BookingStatus)
├── repository/    Spring Data repos (pessimistic lock lives here)
├── service/       EventService, BookingService (transaction boundaries)
├── api/           REST controllers + @RestControllerAdvice error handler
├── web/           Thymeleaf MVC controllers
├── dto/           Request/response records + bean validation
└── exception/     Domain exceptions + JSON error shape
src/main/resources
├── db/migration/  Flyway V1 (schema) + V2 (seed)
├── templates/     Thymeleaf views
└── application.yml
src/test/java/com/stc/tickit
├── service/       BookingServiceTest (Mockito)
└── integration/   BookingIntegrationTest (Testcontainers)
```

## Tech Notes
- Spring Boot version is set to `3.5.5` in `pom.xml`; bump the patch if a newer
  3.5.x is available in your environment.
- This demo keeps authentication out of scope; per the conventions, no credentials are
  hardcoded — the DB password is injected from the environment and never stored in source.
