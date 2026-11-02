# Smart Energy Management System

## Project overview

A Java coursework project that uses electricity import and export prices to select a home battery's target state. Three Micronaut services provide a synthetic tariff API, an IoT device registry, and an event-driven control pipeline. Kafka carries intermediate results; separate MariaDB databases store device data and the latest reactive values.

The project originated from University of York software engineering coursework. It demonstrates asynchronous service integration, relational persistence, container orchestration and testing across service boundaries. [Coursework attribution](#coursework-background-and-attribution) distinguishes the supplied template from subsequent implementation.

## Key features

- REST APIs for rooms, sensors and actuators, plus queries for stored sensor readings.
- Tariff retrieval, current half-hour price extraction, and Kafka-based propagation of battery decisions.
- Battery target-state updates through the IoT REST API, with periodic application to newly created battery actuators.
- Persistent component registration and typed topic slots, exposed through read-only APIs.
- OpenAPI-generated HTTP clients, Flyway migrations, JUnit/Mockito tests and Compose end-to-end tests using Awaitility.

The system updates a database-backed actuator target state; it does not operate physical hardware or model battery charge. The heating simulation remains a starter skeleton. The configured `sensor-readings` topic has no implemented producer or consumer.

## Technology stack

- Java 17; Gradle 8.14.3 wrappers, with a separate build per module.
- Micronaut 4.10.7 (IoT uses 4.10.6); Micronaut Gradle plugins 4.6.1; Netty.
- Kafka (`apache/kafka:4.1.1` in Compose), MariaDB (`mariadb:12`), Hibernate/JPA, HikariCP and Flyway.
- Micronaut OpenAPI and Serialization; JUnit 5, Mockito and Awaitility.
- Docker Compose; Micronaut Test Resources backed by Testcontainers for service-test infrastructure; GitHub Actions.
- Separate Eclipse EMF/Sirius/Epsilon starter tooling under `language/`.

## System architecture and components

See the [Mermaid architecture diagram and source evidence](docs/architecture.md).

- **IoT (8080):** `/rooms`, `/sensors`, `/sensors/{id}/readings` and `/actuators`. Owns rooms, sensors, readings and actuators in its MariaDB database. Migrations seed rooms, temperature sensors, heaters and a home battery.
- **Reactive (8081):** runs the tariff-to-battery pipeline. `/components` exposes registered components; `/topic-slots` and `/topic-slots/topic/{topicName}` expose stored values. Owns a separate MariaDB database.
- **Rates (8082):** serves synthetic Agile-compatible tariff data at `GET /`, including half-hour slots and cheapest-window data. Compose and end-to-end tests use this local source. It has no database.
- **Infrastructure:** one Kafka broker/controller in KRaft mode, two MariaDB instances and Kafka UI on port 9001.

The five reactive components run inside one service process. Kafka connects these components; HTTP connects reactive to the rates source and IoT.

## Kafka communication and event flow

1. The tariff fetcher retrieves JSON, saves it and its validity range, and publishes an `AgileRatesSnapshot` on **`agile-rates`**.
2. Separate import/export consumer groups select the slot where `deliveryStart <= now < deliveryEnd`. They save and publish `CurrentRateSnapshot` values (`rate`, `atTime`) on **`current-import-rate`** and **`current-export-rate`**.
3. The battery decision component consumes both current-rate topics, stores the inputs and waits until both rates exist. Export above `20.0` selects `export_to_grid`; otherwise import below `10.0` selects `import_to_100`; otherwise it selects `sell_excess`. Export takes precedence; comparisons are strict and use the API's numeric rates.
4. It persists the decision and publishes a `BatteryDecisionSnapshot` (`targetState`, `atTime`) on **`battery-decision`**.
5. The actuator consumer lists IoT actuators over HTTP and updates those of type `battery` whose target state differs.

All four producers use the Kafka key `latest`. Topic slots store the latest typed field values in MariaDB, rather than an event history or Kafka offsets. Import/export events arrive independently, so a decision can temporarily combine values from different updates. No transactional outbox, stale-price rejection or exactly-once processing is implemented.

Scheduled work in the active implementation:

- **Tariffs:** fetch on initialisation, then every 12 hours. Retry every 10 seconds after a 5-second initial delay while no tariff JSON is stored.
- **Current rates:** recompute on tariff events and every 30 minutes after a 30-minute initial delay. Retry every 10 seconds after 5 seconds while the corresponding stored rate is missing.
- **Decisions:** react to current-rate events; retry every 10 seconds after 5 seconds while the stored decision is missing.
- **Actuators:** apply on decision events, then reapply every 10 seconds after 5 seconds, updating only mismatched states.
- **Heating:** scheduled at the configured `10s` interval, but its method contains only TODO comments and produces no readings. Tests disable it.

Missing-value retries do not check freshness of existing values. The live tariff fetcher/gateway are disabled in the Micronaut `test` environment; plain unit tests exercise their methods directly.

## Prerequisites

- JDK 17 and Git. A system Gradle installation is unnecessary.
- Running Docker Engine/Desktop with Linux containers and Compose v2 supporting `up --wait`. Check `docker info` and `docker compose version`.
- Internet access for dependencies and container images; direct reactive runs use an external tariff API by default.
- Bash and `curl` for the test script (Git Bash on Windows).
- Free host ports 8080, 8081, 8082, 9001, 9092, 3307 and 3308 for Compose.

## Building and running the project

```sh
git clone https://github.com/AlbaraSh/smart-energy-microservices.git
cd smart-energy-microservices
docker compose -f microservices/compose.yml up --build --wait --wait-timeout 180
docker compose -f microservices/compose.yml ps
docker compose -f microservices/compose.yml logs -f reactive
```

Compose selects each service's `Dockerfile.manual`: a Java 17 multi-stage build running `clean shadowJar`. Image builds package the application without running tests. Databases and Kafka have health checks; application containers have no HTTP health checks, so `up --wait` alone does not establish API readiness.

Once the APIs are ready, inspect the pipeline:

```sh
curl http://localhost:8080/actuators
curl http://localhost:8081/components
curl http://localhost:8081/topic-slots
curl http://localhost:8082/
```

Use `curl.exe` in PowerShell. Inspect the seeded `Home Battery` actuator after startup. Dummy import prices are `10/15/20` and export prices `5/10/15`, so they exercise `sell_excess`; unit tests cover the other decision branches. [Kafka UI](http://localhost:9001) displays the topics. Swagger UI is configured at `/swagger-ui/index.html` on each service, for example [IoT Swagger UI](http://localhost:8080/swagger-ui/index.html).

Stop the development stack, preserving data:

```sh
docker compose -f microservices/compose.yml down
```

Adding `--volumes` deletes its persisted databases and Kafka data. Compose passwords and plaintext single-broker Kafka are local development settings.

### Individual service builds and runs

There is no root Gradle build. From the repository root, for example:

```sh
cd microservices/rates
./gradlew build
./gradlew run
```

PowerShell equivalents are `.\gradlew.bat build` and `.\gradlew.bat run`. The same tasks exist in IoT/reactive. Their `run` tasks use Micronaut Test Resources to provision missing database/Kafka settings through Docker. Reactive also requires IoT at `http://localhost:8080`.

Direct reactive runs default to `https://agilerates.uk/api/agile_rates_region_M.json` (region M/Yorkshire). To use a separately running local rates service, set these before starting reactive:

```sh
export MICRONAUT_HTTP_SERVICES_RATES_URL=http://localhost:8082
export MICRONAUT_HTTP_SERVICES_RATES_PATH=/
```

```powershell
$env:MICRONAUT_HTTP_SERVICES_RATES_URL = 'http://localhost:8082'
$env:MICRONAUT_HTTP_SERVICES_RATES_PATH = '/'
```

Compose sets these overrides automatically. Its Kafka broker advertises `kafka-0:9092`, a container-network address; publishing port 9092 alone does not make it usable by host-side Kafka clients. Use the full Compose stack or Test Resources provisioning for development.

## Running tests

From each service directory (`rates`, `iot`, `reactive`):

```sh
./gradlew test
```

Rates tests require no Docker. IoT/reactive include `@MicronautTest` integration tests using Micronaut Test Resources to provision MariaDB/Kafka through Testcontainers. Their test properties clean the relevant schema: use disposable databases. Reactive also has plain JUnit/Mockito tests for fetchers, consumers, extraction, decisions and actuator updates.

To build/test all services and run full integration tests, from the repository root:

```sh
bash microservices/run-tests.sh
```

The script checks Docker, builds/tests all services, builds and starts an isolated `smart-energy-tests-<pid>` Compose project, waits for HTTP readiness, then runs `end2end-tests`. It prints container logs on failure and removes its own containers/volumes on exit. Stop the development stack first: host ports are shared, although development volumes are separate.

To test an existing running stack instead, run `./gradlew test` from `microservices/end2end-tests` (PowerShell: `.\gradlew.bat test`). Generated HTTP clients target ports 8080/8081. Tests check component registration, topic slots and a battery actuator reaching a valid decision, with Awaitility waits up to 90 seconds. They create devices without removing them, so use a disposable stack. They do not exhaustively test tariff changes or recovery.

HTML reports: each module's `build/reports/tests/test/index.html`; XML: `build/test-results/test/`. `assemble testClasses` packages applications and compiles tests without executing them, useful when Docker is unavailable.

## CI/CD workflow

[GitHub Actions](.github/workflows/gradle.yml) runs on pushes and pull requests to `main` using Ubuntu, Temurin JDK 17 and the Gradle setup action. It configures Docker API `1.44` for Test Resources because the default client API `1.32` was rejected by the hosted runner. It invokes the same test script to build/test all services and run the Compose end-to-end suite, then uploads available HTML/XML reports even after failure. The job timeout is 30 minutes.

The original workflow ran only rates tests; IoT/reactive/end-to-end steps were commented out. Those checks are now enabled through the shared script. This is continuous integration: no image publication or cloud deployment stage is implemented.

## Project structure

```text
.github/workflows/gradle.yml   CI build/test workflow
docs/architecture.md          Mermaid diagram and source evidence
microservices/
  compose.yml                Services, Kafka, Kafka UI and databases
  run-tests.sh               Build/test and disposable integration stack
  rates/                     Synthetic tariff REST service
  iot/                       Device APIs and persistence
  reactive/                  Kafka pipeline, battery control and state APIs
  end2end-tests/             Generated clients and cross-service tests
language/                    Eclipse EMF/Sirius/Epsilon starter projects
model/                       Starter RCL model and Sirius representation
```

Each service has its own build, wrapper, source and tests. IoT/reactive Flyway scripts are under `src/main/resources/db/migration`. HTTP clients are generated from checked-in `src/main/openapi` specifications.

## Coursework background and attribution

This began with the University of York ENG2 2025-26 assessment template for reactive computation and home automation. Initial commit `aa0f0b8` provides the attribution baseline: the dummy rates service, Micronaut/Gradle scaffolding, migrations/seed data, heating skeleton, initial CI/test script and Eclipse language/model scaffolding were supplied.

Subsequent coursework commits implement the IoT REST/domain/repository layers and tests; reactive tariff/Kafka/battery pipeline and state APIs; checked-in component bases and HTTP client specifications; end-to-end battery/state tests; and container configuration. These contributions build on the starter infrastructure. The dummy tariff implementation and starter tooling are not claimed as original work.

Classes under `reactive/.../components/generated` are labelled as generated. Only battery decision/actuator logic currently extends those bases; the active fetcher/extractors use separate implementations. The checked-in RCL model is empty and EGX generation/EVL validation scripts are placeholders. The repository therefore does not provide a reproducible model-to-component generation workflow. Eclipse projects are outside the Gradle/CI build.

## Troubleshooting

- **Docker unavailable:** start Docker Desktop/Engine with Linux containers and confirm `docker info` works. Installing just the CLI is insufficient.
- **Docker Engine 29+ discovery failure:** the starter records [Micronaut Test Resources issue #941](https://github.com/micronaut-projects/micronaut-test-resources/issues/941). If Docker works but Testcontainers reports `Could not find a valid Docker environment`, try the workaround below for this dependency set. Preserve any other existing properties.

  Add/update this line in `~/.docker-java.properties`:

  ```properties
  api.version=1.44
  ```

  If the file does not exist, PowerShell can create it:

  ```powershell
  'api.version=1.44' | Set-Content "$HOME/.docker-java.properties" -Encoding Ascii
  ```

  Reset cached discovery failures with `./gradlew stopTestResourcesService` (PowerShell: `.\gradlew.bat stopTestResourcesService`) in IoT/reactive, then retry.

- **Port conflicts:** stop the development stack before running the integration script; inspect Compose `ps` and service logs.
- **No battery update:** inspect reactive logs and `/topic-slots`. Both current rates must exist. Compose uses dummy rates; direct runs need the external API unless overridden.
- **Stale data:** volumes survive `down`/`up`, and existing slots suppress missing-value retries. For disposable development data, `down --volumes` resets databases and Kafka before starting fresh.
