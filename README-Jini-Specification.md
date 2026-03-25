# GitJini: Jini Network Technology Implementation

This project aims to implement the Jini (now Apache River) network technology based on the official specifications. Jini is a service-oriented architecture that enables dynamic discovery and interaction between services and clients on a network.

## Table of Contents
1. [Architecture Overview](#1-architecture-overview)
2. [Core Components](#2-core-components)
3. [Discovery and Join Protocol](#3-discovery-and-join-protocol)
4. [Lookup Service (LUS)](#4-lookup-service-lus)
5. [Distributed Leasing](#5-distributed-leasing)
6. [Distributed Events](#6-distributed-events)
7. [Implementation Requirements](#7-implementation-requirements)

---

## 1. Architecture Overview
Jini architecture is based on three main concepts: **Services**, **Lookup Services**, and **Clients**.

- **Service**: An entity that provides a specific functionality. It can be a hardware device, software, or a combination of both.
- **Lookup Service (LUS)**: A central directory where services register themselves and clients find services.
- **Client**: An entity that looks for and uses services.

The communication between these entities is handled via **Proxies** (often using Java RMI) that are downloaded from the Lookup Service.

## 2. Core Components
- **Entry**: A typed set of objects used for service attributes.
- **ServiceID**: A 128-bit universally unique identifier for a service.
- **ServiceItem**: A container for a ServiceID, a service object (proxy), and its attributes (Entries).

## 3. Discovery and Join Protocol
Discovery is the process by which a service or client finds a Lookup Service. Join is the process by which a service registers itself with a discovered Lookup Service.

### Protocols:
- **Multicast Request Protocol**: Used by services/clients to find LUSs on the local network.
- **Multicast Announcement Protocol**: Used by LUSs to announce their presence to services/clients on the local network.
- **Unicast Discovery Protocol**: Used to establish communication with a specific LUS when its location is known.

### Join Process:
1. Discover one or more Lookup Services.
2. Register the service proxy and attributes with the LUS.
3. Receive a `ServiceRegistration` which includes a lease.

## 4. Lookup Service (LUS)
The LUS acts as a clearinghouse for services. It supports:
- **Registration**: Services register their proxy and attributes.
- **Lookup**: Clients search for services by type (Java interface) and/or attributes (Entries).
- **Leasing**: All registrations are lease-based to ensure the LUS stays clean of dead services.

## 5. Distributed Leasing
Leasing is a mechanism for resource management in a distributed system.
- Resources (like service registrations) are granted for a specific period.
- The holder of the lease must renew it before it expires to continue using the resource.
- If a lease is not renewed, the resource is automatically reclaimed (e.g., the service is removed from the LUS).

## 6. Distributed Events
Jini provides a distributed event model that extends the Java Event model to a networked environment.
- **Event Generator**: An object that registers listeners for specific events.
- **Remote Event**: An object passed from the generator to the listener when an event occurs.
- **Event Registration**: A lease-based registration of a listener with an event generator.

## 7. Implementation Requirements
To implement Jini, the following are required:
- **Java Runtime**: Support for Java RMI (Remote Method Invocation).
- **Serialization**: Objects must be serializable to be moved across the network.
- **Dynamic Code Loading**: Capability to download and execute service proxies (codebase).
- **Network Stack**: Support for UDP (multicast) and TCP (unicast).

---

## Reference Specifications
- [Jini Architecture Specification](https://river.apache.org/release-doc/current/specs/html/jini-spec.html)
- [Jini Discovery and Join Specification](https://river.apache.org/release-doc/current/specs/html/discovery-spec.html)
- [Jini Lookup Service Specification](https://river.apache.org/release-doc/current/specs/html/lookup-spec.html)
- [Jini Distributed Leasing Specification](https://river.apache.org/release-doc/current/specs/html/lease-spec.html)
- [Jini Distributed Events Specification](https://river.apache.org/release-doc/current/specs/html/event-spec.html)
