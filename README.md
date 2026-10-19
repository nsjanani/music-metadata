# Music Metadata

A service that stores and serves metadata about artists and tracks for a music streaming platform.
Tech Stack: Java 25, Spring Boot 4, MySQL 8.4, built with Maven.

## Features

- **Add a track** to an artist's catalogue (title, genre, length, …)
- **Edit an artist's name**, keeping previous names as aliases
- **Fetch an artist's tracks**
- **Artist of the Day**: a fair, cyclical rotation through the whole artist catalogue

The interface for users is the REST API itself, browsable and testable in Swagger UI; there's no separate web front end.

## Quick start

Run the whole system with Docker (no JDK needed):

```bash
docker compose up --build
```

Then open http://localhost:8080/ to browse and try the API in Swagger UI. Demo artists and tracks are loaded on first start, and every example in the API documentation works against them.

## Repository layout

```
music-metadata/
├── api/
│   └── openapi.yaml    # the API contract (OpenAPI 3.0): the source of truth for the API
├── src/                # the service (Spring Boot)
├── pom.xml             # Maven build; generates the API interfaces from api/openapi.yaml
├── Dockerfile
└── docker-compose.yml  # MySQL + the service, for local use
```

The API is designed first, in `api/openapi.yaml`. The build generates the API interfaces and request/response classes from it, and the controllers implement them, so the code can't drift from the contract. Clients (the platform's apps, other services) can generate their own client code from the same file. To change the API, change the spec first, then regenerate.

## Prerequisites for development

- **JDK 25**
- **Docker** (local MySQL, and the integration tests)

Maven doesn't need to be installed: use the wrapper, `./mvnw`.

## Run locally

Start MySQL:

```bash
docker compose up -d mysql
```

Then start the API with the `local` profile, which connects to that MySQL and loads demo data:

```bash
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

| URL | What |
|---|---|
| http://localhost:8080/ | Swagger UI: browse and try the API. Every example works against the demo data. |
| http://localhost:8080/api/v1/artists/of-the-day | Today's Artist of the Day |
| http://localhost:8080/api/v1/artists | Artists (paged) |
| http://localhost:8080/openapi.yaml | The API contract |
| http://localhost:8080/actuator/health | Health, plus `/liveness` and `/readiness` probes |

MySQL is exposed on port **3307** 
User and password: `music_metadata` / `music_metadata`.

`docker compose stop` stops MySQL and keeps the data; `docker compose down -v` deletes it.

## Run in Docker

`docker compose up --build` runs MySQL and the API together, with the production configuration (JSON logs) and demo data. Stop the API first if it's already running from the IDE: both use port 8080.

To build the image on its own:

```bash
docker build -t music-metadata .
```

The image is multi-stage: a JDK stage builds the app, and the runtime image contains only a JRE and the app, split into layers so a code change only rebuilds the last one. It runs as a non-root user.

## Test

```bash
./mvnw verify
```

runs every test. There are two kinds:

| Kind | Naming | Runs in | Needs Docker | What it checks |
|---|---|---|---|---|
| Unit and slice tests | `*Test` | `./mvnw test` (fast) | No | Services' business rules with mocked repositories; mappers; each controller's HTTP contract with mocked services (`@WebMvcTest`) |
| Integration tests | `*IT` | `./mvnw verify` | Yes | The whole application against a real, throwaway MySQL 8.4 (Testcontainers): business rules, SQL, constraints, concurrency |

Integration tests always use the base (production) configuration, whatever profile your shell or IDE sets.

## Configuration

The base configuration is production-safe: no credentials, no demo data, JSON logs. Supply the database through environment variables:

| Variable | Example |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://db.example.internal:3306/music_metadata` |
| `SPRING_DATASOURCE_USERNAME` | `music_metadata` |
| `SPRING_DATASOURCE_PASSWORD` | from your secret store |

The schema is created and migrated automatically at startup by Liquibase. All dates and times are UTC.

| Profile | Purpose |
|---|---|
| _(none)_ | Production: JSON logs, no demo data |
| `local` | Local development: compose MySQL, demo data, readable logs |

## Working in IntelliJ IDEA

Open the repository folder as a Maven project. Whenever `api/openapi.yaml` changes, run **Maven → Generate Sources and Update Folders**, so the IDE sees the regenerated API classes.
