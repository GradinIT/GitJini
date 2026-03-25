# GitJini Tutorial

Welcome to the GitJini tutorial. This guide explains the core API, the specialized annotations for service discovery, and how the example application demonstrates a "Smart Proxy" with client-side routing.

## 1. Core API Classes

The `jini-core` module implements the foundational Jini specifications. Key classes include:

- **`ServiceID`**: A 128-bit universally unique identifier (UUID) for a service instance.
- **`ServiceItem`**: A container for a `ServiceID`, the service object (typically an RMI proxy), and an array of `Entry` attributes.
- **`ServiceRegistrar`**: The interface for the Lookup Service (LUS). It provides methods to `register` services and `lookup` services based on a `ServiceTemplate`.
- **`ServiceTemplate`**: Used for searching the LUS. It can filter by `ServiceID`, service types (Java interfaces), and specific `Entry` attributes.
- **`Entry`**: A marker interface for service attributes. Attributes are used to describe and find services.
- **`ServiceRegistration`**: Returned when a service registers with an LUS. It contains the `ServiceID` and the `Lease` for the registration.
- **`Lease`**: Represents a time-bound right to a resource (like a service registration). Services must renew their leases periodically to remain active in the LUS.

## 2. Dynamic Discovery & Routing Annotations

GitJini introduces several annotations to simplify service export and import, as well as enabling intelligent routing.

### `@ExportedService`
Used on a service implementation class to mark it for automatic registration.
- **`id`**: (Optional) A base ID for the service.
- **`instanceId`**: (Optional) A unique ID for the specific instance, which is also used as a `RoutingEntry` attribute.

### `@ImportService`
Used on a field in a client or another service to trigger automatic injection of a service proxy. In GitJini, this injections a **Routing Proxy** that handles dynamic lookup and routing.

### `@ServiceRouting`
Used on a field within a request object (payload). When a method is called on an injected proxy, GitJini extracts the value of the field marked with `@ServiceRouting` to determine which service instance should handle the request.

## 3. The Example Application (`jini-example`)

The example demonstrates a complete flow: starting a Lookup Service, registering multiple service instances, and calling them through a routing proxy.

### Components:

1.  **`HelloService`**: A simple RMI interface with a `sayHello(HelloRequest)` method.
2.  **`HelloRequest`**: The payload containing a `name` and a `routingKey` field marked with `@ServiceRouting`.
3.  **`HelloServiceImpl`**: An implementation of `HelloService` annotated with `@ExportedService(instanceId = "instance-1")`.
4.  **`HelloClient`**: A client class that has a `HelloService` field annotated with `@ImportService`.

### Step-by-Step Walkthrough (`JiniExampleApp.java`):

1.  **Start Lookup Service**: A `BasicLookupService` is instantiated and registered in the `DiscoveryService`.
2.  **Create Services**: Two instances of `HelloService` are created with different instance names ("Instance-1" and "Instance-2").
3.  **Automatic Export**: `ServiceExporter.exportIfNeeded()` is called for both instances. It reads the `@ExportedService` annotation and registers them with the LUS, adding a `RoutingEntry` attribute based on the `instanceId`.
4.  **Service Injection**: `ServiceImporter.importServices(helloClient, registrar)` is called. It finds the `@ImportService` field in `HelloClient` and injects a dynamic **Routing Proxy**.
5.  **Routed Calls**:
    - When `helloClient.callHello("Jocke", "World")` is called, the proxy extracts the routing key "Jocke".
    - It hashes the key to select one of the available service instances (e.g., "instance-1" or "instance-2").
    - It then performs a `registrar.lookup()` for a service matching that specific `RoutingEntry`.
    - Finally, it forwards the call to the discovered instance.

## 4. Smart Proxy & Client-Side Routing

GitJini implements "Smart Proxies" using Java's `java.lang.reflect.Proxy`. 

- **Dynamic Lookup**: Instead of holding a static reference to a single service, the proxy performs a lookup on every call (or uses a cached strategy).
- **Sticky Routing**: By using `@ServiceRouting`, related requests (e.g., for the same user or session) can be consistently routed to the same service instance.
- **Load Balancing**: If the specific routing key doesn't match an instance directly, the `ServiceImporter` collects all available instances and picks one deterministically based on the hash of the routing key.

This architecture ensures high availability and allows for stateful or partitioned service processing within a Jini network.
