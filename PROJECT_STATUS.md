# Release status — ServiceDesk 1.0.0

| Verification | Status | Notes |
|---|---|---|
| Java 21 reference server | **PASS** | Compiled with `javac -encoding UTF-8` |
| Seeded fixture loading | **PASS** | 30 tickets and 7 demo users |
| REST API integration tests | **PASS** | 9 tests (health, assets, validation, demo roles, create, assign, state, comment, export, reset grouped across tests) |
| JavaScript syntax | **PASS** | `node --check web/assets/app.js` |
| UI browser rendering | **PASS** | 10 views generated with Chromium; no page JavaScript errors |
| Data persistence | **PASS** | Local runtime TSV file updated and read in integration tests |
| Angular `npm run build` | **NOT VERIFIED** | Dependency registry unavailable during creation |
| Spring Boot `mvn test` | **NOT VERIFIED** | Maven/dependencies unavailable during creation |
| PostgreSQL integration | **NOT VERIFIED** | Optional local Docker Compose configuration |

The browser capture environment blocked navigation to `localhost`. The capture script therefore rendered the exact production web client (`index.html`, `styles.css`, `app.js`) with a **captured HTTP JSON response from the running Java server** supplied to its `fetch()` calls. Screenshot data was not manually invented. The user flows involving POST/PATCH were separately exercised against the actual Java server in the integration tests.

## Release exclusions

No production authentication, actual enterprise SLA calendar, external notifications, real employee data, deployed environment or cloud hosting. The default reference server is single-instance and local-only.
