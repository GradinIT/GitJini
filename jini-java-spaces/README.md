# jini-java-spaces

Lightweight JavaSpaces module for GitJini. It provides the core `JavaSpace` API and an in‑memory reference implementation (`BasicJavaSpace`) guided by the official JavaSpaces Service Specification.

## Status
- Focus: correctness and simplicity for local development and learning
- Scope: single‑JVM, in‑memory store
- Not production ready (see Limitations)

## Features
- Core API `net.jini.space.JavaSpace` with:
  - `write(Entry, Transaction, long lease)`
  - `read(Entry template, Transaction, long timeout)`
  - `take(Entry template, Transaction, long timeout)`
  - `notify(...)`
  - `snapshot(Entry)`
- Extended API `net.jini.space.JavaSpace05` with:
  - `write(List entries, Transaction, List leaseDurations)`
  - `take(Collection templates, Transaction, long timeout, long maxEntries)`
  - `contents(Collection templates, Transaction, long leaseDuration, long maxEntries)`
  - `registerForAvailabilityEvent(...)`
- In‑memory implementation: `net.jini.space.BasicJavaSpace`
- File-based persistence: `net.jini.space.FilePersistenceStore`, `MongoPersistenceStore`, and `JsonPersistenceStore` for durable storage
- Replication for High availability: `net.jini.space.ReplicatingJavaSpace` provides primary-backup replication.
- Jini Lookup integration: `DiscoveryHelper` with automatic discovery support
- `MatchSet` interface with full leasing, snapshot, and basic live updates support
- `registerForAvailabilityEvent` support (as a specialized notification)
- Transaction support (visibility, locks, commit/abort, multi-JVM coordination ready)
- Event notifications per Jini Distributed Events spec
- Field‑based matching per spec semantics (exact value match or wildcard for null fields)
- Lease handling: duration accepted, tracked, and renewable
- Pluggable persistence: `PersistenceStore` interface for custom backends
- `InternalSpaceException` and `UnusableEntriesException` for error reporting

## Getting Started

### Dependency (Maven)
Add the module to your project (the root `pom.xml` already includes it as a child module):

```xml
<dependency>
  <groupId>se.gtradinit</groupId>
  <artifactId>jini-java-spaces</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
```

This module depends on `jini-core` for `Entry`, `Lease`, and transaction stubs.

### Define an Entry
Entries are simple serializable types implementing `net.jini.core.entry.Entry`. Public fields (non-primitive, non-static, non-transient, non-final) participate in matching.

```java
import net.jini.core.entry.Entry;

public class Order implements Entry {
    public String id;     // null = wildcard in templates
    public String item;
    public Integer qty;

    public Order() {} // No-arg constructor required
    public Order(String id, String item, Integer qty) {
        this.id = id;
        this.item = item;
        this.qty = qty;
    }
}
```

### Use the Space

```java
import net.jini.space.BasicJavaSpace;
import net.jini.space.JavaSpace;
import net.jini.core.lease.Lease;
import net.jini.core.transaction.Transaction;

// BasicJavaSpace implements Remote, allowing it to be exported for RMI
JavaSpace space = new BasicJavaSpace();

// Write an entry with a 1-minute lease
Order order = new Order("A-100", "Book", 2);
Lease lease = space.write(order, null, 60_000);

// Read with a template (wildcards via nulls)
Order tmpl = new Order();
tmpl.id = "A-100";   // match specific id, other fields are null (wildcards)
Order found = (Order) space.read(tmpl, null, JavaSpace.NO_WAIT);

// Take (removes the matching entry)
Order taken = (Order) space.take(tmpl, null, JavaSpace.NO_WAIT);
```

### Use Transactions
`BasicJavaSpace` supports transactions to ensure ACID properties across multiple operations.

```java
import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;

// In a real Jini environment, you would obtain a Transaction from a TransactionManager.
Transaction txn = null; // Obtain via TransactionManager
try {
    space.write(order1, txn, Lease.FOREVER);
    space.write(order2, txn, Lease.FOREVER);
    // txn.commit();
} catch (Exception e) {
    // txn.abort();
}
```

Notes:
- Pass a `Transaction` to operations to perform them under transactional control. `BasicJavaSpace` provides `commit(txn)` and `abort(txn)` methods for manual control in this implementation.
- A field set to `null` in the template acts as a wildcard for that field per spec.
- All fields in an `Entry` must be objects (use `Integer` instead of `int`).

### Use Persistence

#### PersistenceStore interface
Custom storage backends can be implemented by implementing the `BasicJavaSpace.PersistenceStore` interface.

#### FilePersistenceStore
Simple binary serialization of entries.
```java
import net.jini.space.BasicJavaSpace;
import net.jini.space.FilePersistenceStore;
import java.io.File;

BasicJavaSpace space = new BasicJavaSpace();
space.setPersistenceStore(new FilePersistenceStore(new File("space-data")));
```

#### MongoPersistenceStore
Store entries in a MongoDB collection. Requires the MongoDB Java driver.
```java
import net.jini.space.BasicJavaSpace;
import net.jini.space.MongoPersistenceStore;

BasicJavaSpace space = new BasicJavaSpace();
space.setPersistenceStore(new MongoPersistenceStore("mongodb://localhost:27017", "jini", "entries"));
```

#### JsonPersistenceStore
Store entries as JSON files using Jackson.
```java
import net.jini.space.BasicJavaSpace;
import net.jini.space.JsonPersistenceStore;
import java.io.File;

BasicJavaSpace space = new BasicJavaSpace();
space.setPersistenceStore(new JsonPersistenceStore(new File("json-data")));
```

### Jini Lookup Registration

```java
import net.jini.space.DiscoveryHelper;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.lookup.ServiceRegistration;

// 1. One-time manual registration (if you already have a registrar)
DiscoveryHelper.register(space, registrar, null, 3600_000);

// 2. Automatic discovery and registration (recommended)
// This will discover all registrars in the default group and register the space with them.
// It also handles registrars discovered in the future.
DiscoveryHelper.AutoRegistration autoReg = DiscoveryHelper.beginAutoRegistration(space, null, null, 3600_000);

// To stop automatic registration later:
// autoReg.terminate();
```

### High Availability (Replication)

`ReplicatingJavaSpace` allows you to keep two spaces in sync by replicating all state-changing operations (write, take) from a primary space to a backup space. it also provides failover for read operations.

```java
import net.jini.space.BasicJavaSpace;
import net.jini.space.ReplicatingJavaSpace;

// BasicJavaSpace and ReplicatingJavaSpace implement Remote, allowing them to be exported as RMI objects
BasicJavaSpace primary = new BasicJavaSpace();
BasicJavaSpace backup = new BasicJavaSpace();
ReplicatingJavaSpace haSpace = new ReplicatingJavaSpace(primary, backup);

// All writes to haSpace are sent to both primary and backup
haSpace.write(new MyEntry("data"), null, 3600_000);

// If primary fails, haSpace will failover to backup for read operations
MyEntry template = new MyEntry();
MyEntry result = (MyEntry) haSpace.read(template, null, 1000);
```

Note: In a production environment, `primary` and `backup` would typically be remote proxies obtained via Jini Discovery or RMI.

## Module Layout
- `src/main/java/net/jini/space/JavaSpace.java` — API
- `src/main/java/net/jini/space/JavaSpace05.java` — Extended API
- `src/main/java/net/jini/space/BasicJavaSpace.java` — In‑memory implementation with transaction, event, and `MatchSet` support
- `src/main/java/net/jini/space/FilePersistenceStore.java` — Durable storage implementation
- `src/main/java/net/jini/space/MongoPersistenceStore.java` — MongoDB-based durable storage
- `src/main/java/net/jini/space/JsonPersistenceStore.java` — JSON-based durable storage
- `src/main/java/net/jini/space/DiscoveryHelper.java` — Jini Lookup integration
- `src/main/java/net/jini/space/ReplicatingJavaSpace.java` — Primary-backup replication implementation
- `src/main/java/net/jini/space/InternalSpaceException.java` — internal error type
- `src/main/java/net/jini/space/MatchSet.java` — interface for batch read results
- `src/main/java/net/jini/entry/UnusableEntriesException.java` — error type for batch operations
- `src/test/java/net/jini/space/BasicJavaSpaceTest.java` — API and logic tests
- `src/test/java/net/jini/space/FilePersistenceTest.java` — Durability tests
- `src/test/java/net/jini/space/ReplicationTest.java` — Replication and failover tests
- `specification.md` — extracted JavaSpaces Service Specification

## Specification
- Local copy: `jini-java-spaces/specification.md`
- Upstream reference: https://river.apache.org/release-doc/current/specs/html/js-spec.html

## Limitations and Roadmap
Current implementation constraints:
- Single‑JVM focus: While `Remote` interfaces are present, high-level cluster management is manual.

Planned improvements:
- Lease renewal management in `AutoRegistration`.

## Build and Test
From the project root:

```bash
mvn -q -pl jini-java-spaces test
```

You should see Surefire report 0 failures for `BasicJavaSpaceTest`.

## License
This module reuses and references materials from the Apache River/JavaSpaces specifications. See headers in `specification.md` and the project’s root license information.
