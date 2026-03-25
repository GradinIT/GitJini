# GitJini: A Simulated Jini & JavaSpaces Environment

GitJini is a simplified, lightweight implementation of the Jini (now Apache River) and JavaSpaces specifications, designed for easy development, testing, and deployment in modern containerized environments like Docker. It supports dynamic service discovery, smart routing, space-based architecture (SBA), and a distributed service grid.

## 1. Core Jini API

The `jini-core` module implements the foundational Jini specifications:

- **`ServiceID`**: A 128-bit UUID for uniquely identifying service instances.
- **`ServiceItem`**: A container for a `ServiceID`, the service proxy, and an array of `Entry` attributes.
- **`ServiceRegistrar`**: The interface for the Lookup Service (LUS). Supports `register` for services and `lookup` for discovery via `ServiceTemplate`.
- **`ServiceTemplate`**: Used for searching the LUS, filtering by ID, Java interfaces, or `Entry` attributes.
- **`Entry`**: A marker interface for service attributes (e.g., `RoutingEntry`).
- **`Lease`**: A time-bound right to a resource. Services must renew leases to remain active in the LUS.

## 2. Dynamic Discovery & Smart Routing

GitJini simplifies service management with annotations and transparent networking:

### Discovery Annotations
- **`@ExportedService`**: Marks a class for automatic registration.
    - `id`: (Optional) Base service ID.
    - `instanceId`: (Optional) If omitted, the LUS dynamically assigns `instance-N`.
- **`@ImportService`**: Triggers injection of a **Routing Proxy** into a field.
- **`@ServiceRouting`**: Marks a field in a request payload to guide the proxy's routing decision.

### Smart Proxy & Client-Side Routing
The `ServiceImporter` injects a `java.lang.reflect.Proxy` that:
1.  **Extracts Routing Keys**: Reads `@ServiceRouting` fields from method arguments.
2.  **Performs Dynamic Lookup**: Queries the LUS for instances matching the routing key (or performs consistent hashing across all matches).
3.  **Sticky Routing**: Ensures related requests (e.g., same session ID) consistently reach the same instance.

## 3. Network-Based Discovery (Docker Support)

When running in a distributed environment (e.g., separate Docker containers), GitJini uses a socket-based **Discovery Server** to bridge isolated JVMs:
- **LUS Container**: Runs a `DiscoveryService` server on port 1099.
- **Remote Clients**: Use `LookupLocator` to connect to the LUS container.
- **Remote Proxy**: The LUS returns a `RemoteServiceRegistrarProxy` that forwards `register` and `lookup` calls over the network.

## 4. JavaSpaces (Space-Based Architecture)

The `jini-java-spaces` module provides a simple implementation of the `JavaSpace` interface:
- **`write(entry, txn, lease)`**: Place an entry into the space.
- **`read(tmpl, txn, timeout)`**: Find a matching entry without removing it.
- **`take(tmpl, txn, timeout)`**: Find and remove a matching entry.
- **Transactional Support**: Basic state management for space operations.
- **Serializable State**: `BasicJavaSpace` is fully serializable, allowing it to be exported as a service to the LUS.

## 5. Jini Service Grid (`jini-grid`)

The grid infrastructure enables automated deployment and management of **Service Units (SU)** across a cluster of **Distributed Service Containers (DSC)**.

### Key Components:
- **Lookup Service (LUS)**: The registry for all grid components and application services.
- **Distributed Service Manager (DSM)**: The "brain" of the grid. It receives deployment requests and distributes instances across available DSCs.
- **Distributed Service Container (DSC)**: The execution environment. It hosts one or more Service Unit instances.

### Service Unit (SU) JAR Structure:
A deployable SU is packaged as a JAR with the following structure:
```text
|----META-INF
|--------spring
|------------pu.xml       (Mandatory: Defines beans and services)
|------------sla.xml      (Optional: Deployment requirements)
|------------pu.properties (Optional: Property overrides)
|----com/mycompany/...    (User classes)
|----lib/                 (Dependencies)
```

### SLA (Service Level Agreement):
SLA XML (e.g., `sla.xml`) controls the deployment topology:
- **`number-of-instances`**: Total primary instances to deploy.
- **`number-of-backups`**: Replicas per primary instance.
- **Cluster Topologies**: Supports `default`, `partitioned`, `sync-replicated`, and `async-replicated`.

## 6. Deployment & Lifecycle

The `DeploymentUtility` CLI (or API) provides full lifecycle control:

- **`deploy <su-path> [sla-path]`**: Loads a SU (directory or JAR), parses the SLA, and instructs the DSM to distribute it.
- **`undeploy <su-name>`**: Removes the service unit from the grid and cancels all LUS leases.
- **`redeploy <su-name>`**: Performs an atomic undeploy and deploy.

### Example Usage:
```bash
# Deploy a Service Unit JAR to the grid
java -cp ... net.jini.grid.DeploymentUtility deploy my-service.jar
```

## 7. Running the Project

### Local Monolithic Run:
Run `JiniExampleApp.main()` to see LUS, Services, and Clients interacting in a single JVM with dynamic ID generation.

### Docker Environment:
```bash
# Build and start the LUS, Service, and Client containers
docker-compose up --build
```
The client container will exit once it successfully discovers and calls the service in the remote container.
