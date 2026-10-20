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

To build the image on its own:

```bash
docker build -t music-metadata .
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

## Test

```bash
./mvnw verify
```

## Working in IntelliJ IDEA

Open the repository folder as a Maven project. Whenever `api/openapi.yaml` changes, run **Maven → Generate Sources and Update Folders**, so the IDE sees the regenerated API classes.
