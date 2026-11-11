# Spring Boot and Angular source modules

These modules extend the zero-dependency Java reference implementation with familiar enterprise frameworks. They are provided as **source code to build and validate on a machine with the required tools**; unlike the Java reference server, their builds were not run during creation of the initial package.

## Spring Boot 4.1 / JPA

Requirements: Java 21+, Maven 3.6.3+, Internet access for dependencies on first build.

From the repository root:

```powershell
mvn -f server/springboot/pom.xml clean test
mvn -f server/springboot/pom.xml spring-boot:run
```

The Spring edition listens on `http://localhost:8081` by default. By default it uses an embedded local H2 **file database** in `data/`. On first run it imports the sample tickets from `data/seed/tickets.tsv` if the table is empty. This edition stores new comments and events via JPA; it doesn't import the reference server's prior events or comments automatically.

For PostgreSQL, start the sample database with Docker Compose:

```powershell
cd server/springboot
docker compose up -d
cd ../..
$env:SPRING_PROFILES_ACTIVE="postgres"
mvn -f server/springboot/pom.xml spring-boot:run
```

Sample Docker Compose credentials are for **isolated local development only** and must be changed before any deployment.

## Angular 21

Requirements: Node 22.12+ and npm; Internet access for first dependency installation.

First start the verified Java reference edition on port 8080, then in a second terminal:

```powershell
cd angular
npm install
npm run build
npm start
```

Open `http://localhost:4200`. The Angular dev server proxies `/api` to `http://127.0.0.1:8080` using `proxy.conf.json`.

To use the Spring edition instead, change the proxy target to `http://127.0.0.1:8081`, restart Angular and ensure Spring Boot is running.

Angular source includes standalone components, typed API models, form handling, ticket filters, admin/technician workflows, user-role switching and dashboard pages. The browser screenshots in `docs/screenshots/` are from the **verified plain-JavaScript web client**, not an Angular production build.

## Important boundaries

The framework modules are an optional implementation path, not proof of framework runtime verification. Do not claim they passed `mvn test` / `ng build` until you have executed those commands on your own machine. The reference Java server and browser client are the tested deliverable for this version.
