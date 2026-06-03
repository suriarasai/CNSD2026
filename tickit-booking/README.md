# TickIt: Containerized Ticket Booking Backend Architecture

This repository contains a Spring Boot 3.5 application utilizing Thymeleaf and PostgreSQL 16, designed to facilitate ticket booking operations. The architecture adheres strictly to the Twelve-Factor App methodology for Infrastructure as Code (IaC) and is orchestrated via Podman Desktop within a Windows 11 environment. 

The system implements three primary user stories: browsing available events, processing ticket reservations (employing pessimistic locking mechanisms to mitigate race conditions and prevent overbooking), and executing booking cancellations with automated seat reallocation. The application exposes both a server-side rendered Thymeleaf User Interface (UI) and a RESTful Application Programming Interface (API) at the `/api/v1/` standard routing path, both of which utilize a unified Controller, Service, and Repository architectural pattern.

Build automation is managed via Maven. A Maven wrapper is included within the repository, eliminating the prerequisite for a local Maven installation.

---

## Technology Stack

| Component | Specification |
|---|---|
| Runtime Environment | Java 21 |
| Core Framework | Spring Boot 3.5.x (Web, Thymeleaf, Data JPA, Validation, Actuator) |
| Relational Database | PostgreSQL 16 (Alpine Linux distribution) |
| Database Migrations | Flyway (Located in `src/main/resources/db/migration`) |
| Build Automation | Maven (via `mvnw` wrapper) |
| Containerization | Podman Desktop / OCI, utilizing a unified `docker-compose.yml` |
| Testing Frameworks | JUnit 5 and Mockito (Unit), Testcontainers (Integration) |

---

## Integrated Development Environment (IDE) Configuration for Lombok

This project utilizes the Lombok library to generate boilerplate code (e.g., `@Getter`, `@Builder`, `@RequiredArgsConstructor`, `@Slf4j`). The Maven build lifecycle is pre-configured to execute the Lombok annotation processor. Consequently, executing `.\\mvnw.cmd clean package` from the command-line interface will succeed without further configuration.

However, your IDE requires explicit Lombok integration. Failure to enable this will result in compilation errors within the editor (e.g., unrecognized variables or undefined builder methods), despite a successful Maven build. 

Please follow the configuration protocol for your specific IDE:

* **Eclipse / Spring Tool Suite (STS):** The Lombok agent must be installed into the IDE executable. Locate the Lombok Java Archive (JAR) file, typically found post-build at `%USERPROFILE%\\.m2\\repository\\org\\projectlombok\\lombok\\<version>\\lombok-<version>.jar`. Execute this file via `java -jar lombok-<version>.jar`. The installer will detect your IDE installation. Select *Install / Update*, restart the IDE, and execute a *Project* > *Clean* operation.
* **IntelliJ IDEA:** Install the *Lombok* plugin (bundled in recent IDE versions). Navigate to *Settings* > *Build, Execution, Deployment* > *Compiler* > *Annotation Processors* and select *Enable annotation processing*.
* **Visual Studio Code (VS Code):** Install the *Lombok Annotations Support* extension (often included in the Extension Pack for Java), and proceed to reload the application window.

To verify that any encountered errors are isolated to the IDE environment, execute `.\\mvnw.cmd clean compile`. A successful compilation confirms the integrity of the codebase.

---

## System Prerequisites

* **Podman Desktop:** Must be operational on Windows 11. Note that Docker Compose syntax is fully supported via the `podman compose` command.
* **Java Development Kit (JDK) 21:** Required only if executing the application on the host machine outside of a container environment (Deployment Protocol B).
* **Maven:** No local installation is required. The `mvnw` or `mvnw.cmd` scripts will automatically provision the correct version upon initial execution.

---

## Deployment Protocol A: Full Stack Containerization

This protocol builds and deploys the application and database concurrently within containerized environments.


```
```powershell
# 1. Provision environment configuration. Never commit the resulting .env file to version control.
copy .env.example .env

# 2. Build and initialize the application and database containers.
podman compose up --build

```

**Routing Endpoints:**

* Application UI: `http://localhost:8080/`
* REST API (Events): `http://localhost:8080/api/v1/events`
* Actuator Health Check: `http://localhost:8080/actuator/health`

**Initialization Sequence:**
The database container initializes first. The application container incorporates a readiness probe and will wait for a successful `pg_isready` health check before booting (Factor IX). Upon application startup, Flyway automatically executes schema creation and data seeding (Factor XII).

To terminate the process, issue an interrupt signal (`Ctrl+C`), followed by `podman compose down`. Append the `-v` flag to purge the persistent data volume.

---

## Deployment Protocol B: Hybrid Execution

This protocol is recommended for iterative development, isolating the database within a container while executing the application directly on the host operating system.

```powershell
# Initialize the PostgreSQL container, persisting data to an explicitly named volume.
podman run -d --name tickit-db `
  -e POSTGRES_DB=tickit -e POSTGRES_USER=tickit -e POSTGRES_PASSWORD=tickit `
  -p 5432:5432 `
  -v tickit-pgdata:/var/lib/postgresql/data `
  postgres:16-alpine

# Execute the application (utilizes the localhost defaults defined in application.yml).
.\\mvnw.cmd spring-boot:run

```

**Database Connection String:** `jdbc:postgresql://localhost:5432/tickit`

---

## Compilation and Test Execution Protocols

```powershell
# Compile the executable artifact (bypassing test execution)
.\\mvnw.cmd clean package -DskipTests

# Execute the comprehensive test suite
.\\mvnw.cmd test

# Execute isolated unit tests (container runtime not required)
.\\mvnw.cmd test -Dtest=BookingServiceTest

```

**Test Typologies:**

* **`BookingServiceTest`:** Isolated unit tests utilizing Mockito. No container engine is required. These verify domain logic including seat decrement calculation, overbooking rejection, and seat release during cancellation.
* **`BookingIntegrationTest`:** Full-stack integration tests executed against an ephemeral PostgreSQL 16 container provisioned via Testcontainers. **Requires an active Podman or Docker runtime.** This includes a concurrent thread execution test to validate the efficacy of the pessimistic write lock against race conditions.

**Note on Testcontainers:** The framework communicates with the container engine via a socket interface. When utilizing Podman Desktop, ensure the Docker-compatible socket is enabled (*Settings* > *Docker compatibility*), or explicitly define the `DOCKER_HOST` environment variable to point to the Podman machine socket.

The compilation process outputs an executable Java Archive at `target/tickit-booking-0.0.1-SNAPSHOT.jar`, which can be executed directly via `java -jar target/tickit-booking-0.0.1-SNAPSHOT.jar`.

---

## Application Programming Interface (API) Specification

| HTTP Method | Endpoint Route | Associated User Story | Functional Description |
| --- | --- | --- | --- |
| `GET` | `/api/v1/events` | US1 | Retrieves upcoming events with available capacity. |
| `POST` | `/api/v1/bookings` | US2 | Processes a booking. Expected payload: `{eventId, customerName, customerEmail, quantity}` |
| `DELETE` | `/api/v1/bookings/{id}` | US3 | Cancels a booking and reallocates reserved seats. |
| `GET` | `/api/v1/bookings?email=` | N/A | Retrieves historical booking records for a specified customer. |

**Execution Example:**

```powershell
curl -X POST http://localhost:8080/api/v1/bookings `
  -H "Content-Type: application/json" `
  -d '{"eventId":1,"customerName":"Jane","customerEmail":"jane@example.com","quantity":2}'

```

---

## Implementation of the Twelve-Factor App Methodology

| Factor | Implementation Strategy |
| --- | --- |
| I. Codebase | Utilizes a unified `docker-compose.yml`; avoids environment-specific codebase forks. |
| II. Dependencies | Employs pinned dependency tags (e.g., `postgres:16-alpine`, `maven:3.9.9-eclipse-temurin-21`). |
| III. Config | Isolates sensitive variables in a `.env` file (excluded from version control); ingested via environment variables. |
| IV. Backing Services | Connects the database via the `SPRING_DATASOURCE_URL` abstraction. |
| V. Build, Release, Run | Utilizes a multi-stage `Dockerfile` to separate the build context from the runtime environment. |
| VI. Processes | Maintains a stateless application architecture; operates under non-root permissions; avoids local session storage. |
| VII. Port Binding | Maps external port `8080` to internal container port `8080`. |
| VIII. Concurrency | Facilitates horizontal scaling of the stateless process (e.g., `podman compose up --scale app=N`*). |
| IX. Disposability | Implements `pg_isready` probes, Spring Actuator health checks, and graceful shutdown procedures. |
| X. Dev/Prod Parity | Maintains identical base images and database versions (PostgreSQL 16) across testing and runtime environments. |
| XI. Logs | Directs application logging exclusively to standard output (stdout) and standard error (stderr) streams. |
| XII. Admin Processes | Executes Flyway database migrations as an automated initialization hook. |

** Note: To scale successfully, static `container_name` and host port mappings must be removed from the target container configuration.*

---

## Codebase Structure and Component Organization

```text
pom.xml              Maven Project Object Model (Build Configuration)
mvnw / mvnw.cmd      Maven Wrapper Executables
src/main/java/com/stc/tickit
├── domain/        Java Persistence API (JPA) Entities (Event, Booking, AppUser)
├── repository/    Spring Data Repositories (Housing the pessimistic lock queries)
├── service/       Business Logic and Transaction Boundaries
├── api/           REST Controllers and Global Exception Handling (@RestControllerAdvice)
├── web/           Thymeleaf Model-View-Controller (MVC) Controllers
├── dto/           Data Transfer Objects and Bean Validation Constraints
└── exception/     Domain-Specific Exceptions and JSON Error Definitions
src/main/resources
├── db/migration/  Flyway SQL Scripts (V1 Schema Initialization, V2 Data Seeding)
├── templates/     Thymeleaf HTML Views
└── application.yml  Application Configuration Properties
src/test/java/com/stc/tickit
├── service/       Unit Testing Implementations (Mockito)
└── integration/   Integration Testing Implementations (Testcontainers)

```

## Technical Addenda and System Constraints

* The Spring Boot version is explicitly declared as `3.5.5` within the `pom.xml`. Administrators should increment the patch version if a more secure 3.5.x release becomes available in their operational environment.
* User authentication mechanisms are outside the scope of this baseline architecture. Following security best practices, no credentials are hardcoded within the source; database initialization credentials are fundamentally decoupled and injected exclusively via the runtime environment.
"""
