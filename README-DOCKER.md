# Jini Docker Example

This example demonstrates how to deploy a Jini Lookup Service (LUS) and a Jini Service in separate Docker containers.

## Prerequisites

- Docker and Docker Compose installed.
- Maven (to build the project).

## Building

First, build the project and create the fat JAR for the example:

```bash
mvn clean install -DskipTests
```

## Running with Docker Compose

To start the LUS and the service:

```bash
docker-compose up --build
```

This will start three containers:
1. `lus`: The Jini Lookup Service, listening on port 10999.
2. `service`: A Jini service that automatically registers with the `lus`.
3. `client`: A Jini client that connects to the `lus`, finds the `service`, and calls it.

## Architecture

- **Java Version**: The containers run **Java 25** (using `openjdk:25-ea-jdk-slim`).
- **LUS (Lookup Service)**: Acts as the service registry. It uses `BasicLookupService`.
- **Service**: A `HelloService` implementation that uses `@ExportedService` for automatic registration.
- **Client**: Uses `ServiceImporter` to find and call the `HelloService`.

The containers communicate over a dedicated Docker bridge network (`jini-network`). The `service` and `client` are configured to find the LUS at the hostname `lus`.

## Docker Enforcement

The `JiniExampleApp` is configured to **enforce running inside Docker** when started in modular mode (`lus`, `service`, or `client`). If you attempt to run it manually outside of a container, it will exit with an error.

To bypass this for local development, you can use the monolithic mode by running without arguments:
```bash
java -jar jini-example/target/jini-example-1.0-SNAPSHOT.jar
```
This mode does not check for Docker.

## Manual execution (Optional)

If you want to run the JAR manually without Docker:

### Start LUS
```bash
java -Dlus.host=localhost -Dlus.port=10999 -jar jini-example/target/jini-example-1.0-SNAPSHOT.jar lus
```

### Start Service
```bash
java -Dlus.host=localhost -Dlus.port=10999 -jar jini-example/target/jini-example-1.0-SNAPSHOT.jar service
```

### Start Client
```bash
java -Dlus.host=localhost -Dlus.port=10999 -jar jini-example/target/jini-example-1.0-SNAPSHOT.jar client
```
