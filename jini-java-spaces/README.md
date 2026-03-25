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
- `MatchSet` interface for exhaustive reading of entries
- Transaction support (visibility, locks, commit/abort)
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
Entries are simple serializable types implementing `net.jini.core.entry.Entry`. Public fields participate in matching.

```java
import net.jini.core.entry.Entry;

public class Order implements Entry {
    public String id;     // null = wildcard in templates
    public String item;
    public Integer qty;
}
```

### Use the Space

```java
import net.jini.space.BasicJavaSpace;
import net.jini.space.JavaSpace;
import net.jini.core.lease.Lease;

JavaSpace space = new BasicJavaSpace();

// Write an entry with a 1-minute lease
Order order = new Order();
order.id = "A-100";
order.item = "Book";
order.qty = 2;
Lease lease = space.write(order, null, 60_000);

// Read with a template (wildcards via nulls)
Order tmpl = new Order();
tmpl.id = "A-100";   // match specific id
Order found = (Order) space.read(tmpl, null, JavaSpace.NO_WAIT);

// Take (removes the matching entry)
Order taken = (Order) space.take(tmpl, null, JavaSpace.NO_WAIT);
```

Notes:
- Pass a `Transaction` to operations to perform them under transactional control. `BasicJavaSpace` provides `commit(txn)` and `abort(txn)` methods for manual control in this implementation.
- A field set to `null` in the template acts as a wildcard for that field per spec.

## Module Layout
- `src/main/java/net/jini/space/JavaSpace.java` — API
- `src/main/java/net/jini/space/JavaSpace05.java` — Extended API (batch ops, availability events)
- `src/main/java/net/jini/space/BasicJavaSpace.java` — In‑memory implementation with transaction, event, and `MatchSet` support
- `src/main/java/net/jini/space/InternalSpaceException.java` — internal error type
- `src/main/java/net/jini/space/MatchSet.java` — interface for batch read results
- `src/main/java/net/jini/entry/UnusableEntriesException.java` — error type for batch operations
- `src/test/java/net/jini/space/BasicJavaSpaceTest.java` — Comprehensive usage tests including transactions, events, and batch operations
- `specification.md` — extracted JavaSpaces Service Specification (text version)

## Specification
- Local copy: `jini-java-spaces/specification.md`
- Upstream reference: https://river.apache.org/release-doc/current/specs/html/js-spec.html

The design follows the spec’s core operations (JS.2) and entry matching semantics. Some advanced aspects are intentionally deferred (see below).

## Limitations and Roadmap
Current implementation constraints:
- In‑memory defaults: By default, data is lost on JVM exit unless a `PersistenceStore` is provided.
- Single‑JVM: The `BasicJavaSpace` is intended for use within a single JVM or via local RMI proxies.
- No replication: High availability via replication is not part of `BasicJavaSpace`.

Planned improvements:
- Multi‑JVM transaction coordination (distributed transaction manager support).
- More robust `PersistenceStore` implementations (e.g., SQLite, JSON).
- Enhanced Jini Discovery and Lookup integration.
- Full `MatchSet` implementation with remote leasing and live updates.

## Build and Test
From the project root:

```bash
mvn -q -pl jini-java-spaces test
```

You should see Surefire report 0 failures for `BasicJavaSpaceTest`.

## License
This module reuses and references materials from the Apache River/JavaSpaces specifications. See headers in `specification.md` and the project’s root license information.
