# JavaSpaces Service Specification
Spec Index
JavaSpaces Service Specification
Version 2.3
   JS - JavaSpacesTM Service Specification
   JS.1	 Introduction	 
   Distributed systems are hard to build. They require careful thinking about problems that do not occur in local computation. The primary problems are those of partial failure, greatly increased latency, and language compatibility. The Java(TM) programming language has a remote method invocation system called RMI that lets you approach general distributed computation in the Java(TM) programming language using techniques natural to the Java programming language and application environment. This is layered on the Java platform's object serialization mechanism to marshal parameters of remote methods into a form that can be shipped across the wire and unmarshalled in a remote server's Java virtual machine1 (JVM).
   This specification describes the architecture of JavaSpaces technology, which is designed to help you solve two related problems: distributed persistence and the design of distributed algorithms. JavaSpaces services use RMI and the serialization feature of the Java programming language to accomplish these goals.
   JS.1.1	 The JavaSpaces Application Model and Terms
   A JavaSpaces service holds entries. An entry is a typed group of objects, expressed in a class for the Java platform that implements the interface net.jini.core.entry.Entry. Entries are described in detail in the Jini Entry Specification. 
   An entry can be written into a JavaSpaces service, which creates a copy of that entry in the space2 that can be used in future lookup operations.
   You can look up entries in a JavaSpaces service using templates, which are entry objects that have some or all of its fields set to specified values that must be matched exactly. Remaining fields are left as wildcards--these fields are not used in the lookup.
   There are two kinds of lookup operations: read and take. A read request to a space returns either an entry that matches the template on which the read is done, or an indication that no match was found. A take request operates like a read, but if a match is found, the matching entry is removed from the space.
   You can request a JavaSpaces service to notify you when an entry that matches a specified template is written. This is done using the distributed event model contained in the package net.jini.core.event and described in the Jini Distributed Events Specification.
   All operations that modify a JavaSpaces service are performed in a transactionally secure manner with respect to that space. That is, if a write operation returns successfully, that entry was written into the space (although an intervening take may remove it from the space before a subsequent lookup of yours). And if a take operation returns an entry, that entry has been removed from the space, and no future operation will read or take the same entry. In other words, each entry in the space can be taken at most once. Note, however, that two or more entries in a space may have exactly the same value.
   The architecture of JavaSpaces technology supports a simple transaction mechanism that allows multi-operation and/or multi-space updates to complete atomically. This is done using the two-phase commit model under the default transaction semantics, as defined in the package net.jini.core.transaction and described in the Jini Transaction Specification.
   Entries written into a JavaSpaces service are governed by a lease, as defined in the package net.jini.core.lease and described in the Jini Distributed Leasing Specification.
   JS.1.1.1	 Distributed Persistence
   Implementations of JavaSpaces technology provide a mechanism for storing a group of related objects and retrieving them based on a value-matching lookup for specified fields. This allows a JavaSpaces service to be used to store and retrieve objects on a remote system.
   JS.1.1.2	 Distributed Algorithms as Flows of Objects
   Many distributed algorithms can be modeled as a flow of objects between participants. This is different from the traditional way of approaching distributed computing, which is to create method-invocation-style protocols between participants. In this architecture's "flow of objects" approach, protocols are based on the movement of objects into and out of implementations of JavaSpaces technology.
   For example, a book-ordering system might look like this:
   A book buyer wants to buy 100 copies of a book. The buyer writes a request for bids into a particular public JavaSpaces service.
   The broker runs a server that takes those requests out of the space and writes them into a JavaSpaces service for each book seller who registered with the broker for that service.
   A server at each book seller removes the requests from its JavaSpaces service, presents the request to a human to prepare a bid, and writes the bid into the space specified in the book buyer's request for bids.
   When the bidding period closes, the buyer takes all the bids from the space and presents them to a human to select the winning bid.
   A method-invocation-style design would create particular remote interfaces for these interactions. With a "flow of objects" approach, only one interface is required: the net.jini.space.JavaSpace interface.
   In general, the JavaSpaces application world looks like this:
   Clients perform operations that map entries or templates onto JavaSpaces services. These can be singleton operations (as with the upper client), or contained in transactions (as with the lower client) so that all or none of the operations take place. A single client can interact with as many spaces as it needs to. Identities are accessed from the security subsystem and passed as parameters to method invocations. Notifications go to event catchers, which may be clients themselves or proxies for a client (such as a store-and-forward mailbox).
   JS.1.2	 Benefits
   JavaSpaces services are tools for building distributed protocols. They are designed to work with applications that can model themselves as flows of objects through one or more servers. If your application can be modeled this way, JavaSpaces technology will provide many benefits.
   JavaSpaces services can provide a reliable distributed storage system for the objects. In the book-buying example, the designer of the system had to define the protocol for the participants and design the various kinds of entries that must be passed around. This effort is akin to designing the remote interfaces that an equivalent customized service would require. Both the JavaSpaces system solution and the customized solution would require someone to write the code that presented requests and bids to humans in a GUI. And in both systems, someone would have to write code to handle the seller's registrations of interest with the broker.
   The server for the model that uses the JavaSpaces API would be implemented at that point.
   The customized system would need to implement the servers. These servers would have to handle concurrent access from multiple clients. Someone would need to design and implement a reliable storage strategy that guaranteed the entries written to the server would not be lost in an unrecoverable or undetectable way. If multiple bids needed to be made atomically, a distributed transaction system would have to be implemented.
   All these concerns are solved in JavaSpaces services. They handle concurrent access. They store and retrieve entries atomically. And they provide an implementation of the distributed transaction mechanism.
   This is the power of the JavaSpaces technology architecture--many common needs are addressed in a simple platform that can be easily understood and used in powerful ways.
   JavaSpaces services also help with data that would traditionally be stored in a file system, such as user preferences, e-mail messages, and images. Actually, this is not a different use of a JavaSpaces service. Such uses of a file system can equally be viewed as passing objects that contain state from one external object (the image editor) to another (the window system that uses the image as a screen background). And JavaSpaces services enhance this functionality because they store objects, not just data, so the image can have abstract behavior, not just information that must be interpreted by some external application(s).
   JavaSpaces services can provide distributed object persistence with objects in the Java programming language. Because code written in the Java programming language is downloadable, entries can store objects whose behavior will be transmitted from the writer to the readers, just as in an RMI using Java technology. An entry in a space may, when fetched, cause some active behavior in the reading client. This is the benefit of storing objects, not just data, in an accessible repository for distributed cooperative computing.
   JS.1.3	 JavaSpaces Technology and Databases
   A JavaSpaces service can store persistent data which is later searchable. But a JavaSpaces service is not a relational or object database. JavaSpaces services are designed to help solve problems in distributed computing, not to be used primarily as a data repository (although there are many data storage uses for JavaSpaces applications). Some important differences are:
   Relational databases understand the data they store and manipulate it directly via query languages. JavaSpaces services store entries that they understand only by type and the serialized form of each field. There are no general queries in the JavaSpaces application design, only "exact match" or "don't care" for a given field. You design your flow of objects so that this is sufficient and powerful.
   Object databases provide an object oriented image of stored data that can be modified and used, nearly as if it were transient memory. JavaSpaces systems do not provide a nearly transparent persistent/transient layer, and work only on copies of entries.
   These differences exist because JavaSpaces services are designed for a different purpose than either relational or object databases. A JavaSpaces service can be used for simple persistent storage, such as storing a user's preferences that can be looked up by the user's ID or name. JavaSpaces service functionality is somewhere between that of a filesystem and a database, but it is neither.
   JS.1.4	 JavaSpaces System Design and Linda3 Systems
   The JavaSpaces system design is strongly influenced by Linda systems, which support a similar model of entry-based shared concurrent processing. In Section&nbsp;JS.4.1, "Linda Systems", you will find several references that describe Linda-style systems.
   No knowledge of Linda systems is required to understand this specification. This section discusses the relationship of JavaSpaces systems with respect to Linda systems for the benefit of those already familiar with Linda programming. Other readers should feel free to skip ahead.
   JavaSpaces systems are similar to Linda systems in that they store collections of information for future computation and are driven by value-based lookup. They differ in some important ways:
   Linda systems have not used rich typing. JavaSpaces systems take a deep concern with typing from the Java platform type-safe environment. In JavaSpaces systems, entries themselves, not just their fields, are typed--two different entries with the same field types but with different data types for the Java programming language are different entry types. For example, an entry that had a string and two double values could be either a idd point or a idd vector. In JavaSpaces systems these two entry types would have specific different classes for the Java platform, and templates for one type would never match the other, even if the values were compatible.
   Entries are typed as objects in the Java programming language, so they may have methods associated with them. This provides a way of associating behavior with entries.
   As another result of typed entries, JavaSpaces services allow matching of subtypes--a template match can return a type that is a subtype of the template type. This means that the read or take may return more states than anticipated. In combination with the previous point, this means that entry behavior can be polymorphic in the usual object-oriented style that the Java platform provides.
   The fields of entries are objects in the Java programming language. Any object data type for the Java programming language can be used as a template for matching entry lookups as long as it has certain properties. This means that computing systems constructed using the JavaSpaces API are object-oriented from top to bottom, and behavior-based (agent-like) applications can use JavaSpaces services for co-ordination.
   Most environments will have more than one JavaSpaces service. Most Linda tuple spaces have one tuple space for all cooperating threads. So transactions in the JavaSpaces system can span multiple spaces (and even non-JavaSpaces system transaction participants).
   Entries written into a JavaSpaces service are leased. This helps keep the space free of debris left behind due to system crashes and network failures.
   The JavaSpaces API does not provide an equivalent of "eval" because it would require the service to execute arbitrary computation on behalf of the client. Such a general compute service has its own large number of requirements (such as security and fairness).
   On the nomenclature side, the JavaSpaces technology API uses a more accessible set of terms than the traditional Linda terms. The term mappings are "entry" for "tuple," "value" for "actual," "wildcard" for "formal," "write" for "out," and "take" for "in." So the Linda sentence "When you `out' a tuple make sure that actuals and formals in `in' and `read' can do appropriate matching" would be translated to "When you write an entry make sure that values and wildcards in take and read can do appropriate matching."
   JS.1.5	 Goals and Requirements
   The goals for the design of JavaSpaces technology are:
   Provide a platform for designing distributed computing systems that simplifies the design and implementation of those systems.
   The client side should have few classes, both to keep the client-side model simple and to make downloading of the client classes quick.
   The client side should have a small footprint, because it will run on computers with limited local memory.
   A variety of implementations should be possible, including relational database storage and object-oriented database storage.
   It should be possible to create a replicated JavaSpaces service.
   The requirements for JavaSpaces application clients are:
   It must be possible to write a client purely in the Java programming language.
   Clients must be oblivious to the implementation details of the service. The same entries and templates must work in the same ways no matter which implementation is used.
   JS.1.6	 Dependencies
   This document relies upon the following other specifications:
   JavaTM Object Serialization Specification 
   JavaTM Remote Method Invocation Specification 
   JiniTM Entry Specification
   JiniTM Distributed Events Specification
   JiniTM Distributed Leasing Specification
   JiniTM Transaction Specification
   JS.2	 Operations
  There are four primary kinds of operations that you can invoke on a JavaSpaces service. Each operation has parameters that are entries, including some that are templates, which are a kind of entry. This chapter describes entries, templates, and the details of the operations, which are:
   write: Write the given entry into this JavaSpaces service.
   read: Read an entry from this JavaSpaces service that matches the given template.
   take: Read an entry from this JavaSpaces service that matches the given template, removing it from this space.
   notify: Notify a specified object when entries that match the given template are written into this JavaSpaces service.
   As used in this document, the term "operation" refers to a single invocation of a method; for example, two different take operations may have different templates.
   JS.2.1	 Entries
   The types Entry and UnusableEntryException that are used in this specification are from the package net.jini.core.entry and are described in detail in the Jini Entry Specification. In the terminology of that specification write is a store operation; read and take are combination search and fetch operations; and notify sets up repeated search operations as entries are written to the space.
   JS.2.2	 net.jini.space.JavaSpace 
   All operations are invoked on an object that implements the JavaSpace interface. For example, the following code fragment would write an entry of type AttrEntry into the JavaSpaces service referred to by the identifier space:
JavaSpace space = getSpace();
AttrEntry e = new AttrEntry();
e.id = "Duke";
e.value = new GIFImage("dukeWave.gif");
space.write(e, null, 60 * 60 * 1000);	 // one hour
// lease is ignored -- one hour will be enough
   The JavaSpace interface is:
package net.jini.space;
import java.rmi.*;
import net.jini.core.event.*;
import net.jini.core.transaction.*;
import net.jini.core.lease.*;
public interface JavaSpace {
    Lease write(Entry e, Transaction txn, long lease)
        throws RemoteException, TransactionException;
    public final long NO_WAIT = 0; // don't wait at all
    Entry read(Entry tmpl, Transaction txn, long timeout)
        throws TransactionException, UnusableEntryException,
               RemoteException, InterruptedException;
    Entry readIfExists(Entry tmpl, Transaction txn, 
                       long timeout)
        throws TransactionException, UnusableEntryException,
               RemoteException, InterruptedException;
    Entry take(Entry tmpl, Transaction txn, long timeout)
        throws TransactionException, UnusableEntryException,
               RemoteException, InterruptedException;
    Entry takeIfExists(Entry tmpl, Transaction txn, 
                       long timeout)
        throws TransactionException, UnusableEntryException,
               RemoteException, InterruptedException;
    EventRegistration notify(Entry tmpl, Transaction txn,
              RemoteEventListener listener, long lease,
              MarshalledObject handback)
        throws RemoteException, TransactionException;
    Entry snapshot(Entry e) throws RemoteException;
}
   The Transaction and TransactionException types in the above signatures are imported from net.jini.core.transaction. The Lease type is imported from net.jini.core.lease. The RemoteEventListener and EventRegistration types are imported from net.jini.core.event.
   In all methods that have the parameter, txn may be null, which means that no Transaction object is managing the operation (see Section&nbsp;JS.3, "Transactions"). 
   The JavaSpace interface is not a remote interface. Each implementation of a JavaSpaces service exports proxy objects that implement the JavaSpace interface locally on the client, talking to the actual JavaSpaces service through an implementation-specific interface. An implementation of any JavaSpace method may communicate with a remote JavaSpaces service to accomplish its goal; hence, each method throws RemoteException to allow for possible failures. Unless noted otherwise in this specification, when you invoke JavaSpace methods you should expect RemoteExceptions on method calls in the same cases in which you would expect them for methods invoked directly on an RMI remote reference. For example, invoking snapshot might require talking to the remote JavaSpaces server, and so might get a RemoteException if the server crashes during the operation.
   The details of each JavaSpace method are given in the sections that follow.
   JS.2.2.1	 InternalSpaceException 
   The exception InternalSpaceException may be thrown by a JavaSpaces service that encounters an inconsistency in its own internal state or is unable to process a request because of internal limitations (such as storage space being exhausted). This exception is a subclass of RuntimeException. The exception has two constructors: one that takes a String description and another that takes a String and a nested exception; both constructors simply invoke the RuntimeException constructor that takes a String argument.
package net.jini.space;
public class InternalSpaceException extends RuntimeException {
    public final Throwable nestedException;
    public InternalSpaceException(String msg) {...}
    public InternalSpaceException(String msg, Throwable e) {...}
    public printStackTrace() {...}
    public printStackTrace(PrintStream out) {...}
    public printStackTrace(PrintWriter out) {...}
}
   The nestedException field is the one passed to the second constructor, or null if the first constructor was used. The overridden printStackTrace methods print out the stack trace of the exception and, if nestedException is not null, print out that stack trace as well.
   JS.2.3	 write 
   A write places a copy of an entry into the given JavaSpaces service. The Entry passed to the write is not affected by the operation. Each write operation places a new entry into the specified space, even if the same Entry object is used in more than one write.
   Each write invocation returns a Lease object that is lease milliseconds long. If the requested time is longer than the space is willing to grant, you will get a lease with a reduced time. When the lease expires, the entry is removed from the space. An IllegalArgumentException will be thrown if the lease time requested is negative and not equal to Lease.ANY.
   If a write returns without throwing an exception, that entry is committed to the space, possibly within a transaction (see Section&nbsp;JS.3, "Transactions"). If a RemoteException is thrown, the write may or may not have been successful. If any other exception is thrown, the entry was not written into the space.
   Writing an entry into a space might generate notifications to registered objects (see Section&nbsp;JS.2.7, "notify").
   JS.2.4	 readIfExists and read
   The two forms of the read request search the JavaSpaces service for an entry that matches the template provided as an Entry. If a match is found, a reference to a copy of the matching entry is returned. If no match is found, null is returned. Passing a null reference for the template will match any entry.
   Any matching entry can be returned. Successive read requests with the same template in the same JavaSpaces service may or may not return equivalent objects, even if no intervening modifications have been made to the space. Each invocation of read may return a new object even if the same entry is matched in the JavaSpaces service.
   A readIfExists request will return a matching entry, or null if there is currently no matching entry in the space. If the only possible matches for the template have conflicting locks from one or more other transactions, the timeout value specifies how long the client is willing to wait for interfering transactions to settle before returning a value. If at the end of that time no value can be returned that would not interfere with transactional state, null is returned. Note that, due to the remote nature of JavaSpaces services, read and readIfExists may throw a RemoteException if the network or server fails prior to the timeout expiration
   A read request acts like a readIfExists except that it will wait until a matching entry is found or until transactions settle, whichever is longer, up to the timeout period.
   In both read methods, a timeout of NO_WAIT means to return immediately, with no waiting, which is equivalent to using a zero timeout. An IllegalArgumentException will be thrown if a negative timeout value is used.
   JS.2.5	 takeIfExists and take
   The take requests perform exactly like the corresponding read requests (see Section&nbsp;JS.2.4, "readIfExists and read"), except that the matching entry is removed from the space. Two take operations will never return copies of the same entry, although if two equivalent entries were in the JavaSpaces service the two take operations could return equivalent entries.
   If a take returns a non-null value, the entry has been removed from the space, possibly within a transaction (see Section&nbsp;JS.3, "Transactions"). This modifies the claims to once-only retrieval: A take is considered to be successful only if all enclosing transactions commit successfully. If a RemoteException is thrown, the take may or may not have been successful. If an UnusableEntryException is thrown, the take removed the unusable entry from the space; the contents of the exception are as described in the Jini Entry Specification. If any other exception is thrown, the take did not occur, and no entry was removed from the space.
   With a RemoteException, an entry can be removed from a space and yet never returned to the client that performed the take, thus losing the entry in between. In circumstances in which this is unacceptable, the take can be wrapped inside a transaction that is committed by the client when it has the requested entry in hand.
   JS.2.6	 snapshot 
   The process of serializing an entry for transmission to a JavaSpaces service will be identical if the same entry is used twice. This is most likely to be an issue with templates that are used repeatedly to search for entries with read or take. The client-side implementations of read and take cannot reasonably avoid this duplicated effort, since they have no efficient way of checking whether the same template is being used without intervening modification.
   The snapshot method gives the JavaSpaces service implementor a way to reduce the impact of repeated use of the same entry. Invoking snapshot with an Entry will return another Entry object that contains a snapshot of the original entry. Using the returned snapshot entry is equivalent to using the unmodified original entry in all operations on the same JavaSpaces service. Modifications to the original entry will not affect the snapshot. You can snapshot a null template; snapshot may or may not return null given a null template.
   The entry returned from snapshot will be guaranteed equivalent to the original unmodified object only when used with the space. Using the snapshot with any other JavaSpaces service will generate an IllegalArgumentException unless the other space can use it because of knowledge about the JavaSpaces service that generated the snapshot. The snapshot will be a different object from the original, may or may not have the same hash code, and equals may or may not return true when invoked with the original object, even if the original object is unmodified.
   A snapshot is guaranteed to work only within the virtual machine in which it was generated. If a snapshot is passed to another virtual machine (for example, in a parameter of an RMI call), using it--even with the same JavaSpaces service--may generate an IllegalArgumentException.
   We expect that an implementation of JavaSpaces technology will return a specialized Entry object that represents a pre-serialized version of the object, either in the object itself or as an identifier for the entry that has been cached on the server. Although the client may cache the snapshot on the server, it must guarantee that the snapshot returned to the client code is always valid. The implementation may not throw any exception that indicates that the snapshot has become invalid because it has been evicted from a cache. An implementation that uses a server-side cache must therefore guarantee that the snapshot is valid as long as it is reachable (not garbage) in the client, such as by storing enough information in the client to be able to re-insert the snapshot into the server-side cache.
   No other method returns a snapshot. Specifically, the return values of the read and take methods are not snapshots and are usable with any implementation of JavaSpaces technology.
   JS.2.7	 notify 
   A notify request registers interest in future incoming entries to the JavaSpaces service that match the specified template. Matching is done as it is for read. The notify method is a particular registration method under the Jini Distributed Events Specification. When matching entries are written, the specified RemoteEventListener will eventually be notified. When you invoke notify you provide an upper bound on the lease time, which is how long you want the registration to be remembered by the JavaSpaces service. The service decides the actual time for the lease. You will get an IllegalArgumentException if the lease time requested is not Lease.ANY and is negative. The lease time is expressed in the standard millisecond units, although actual lease times will usually be of much larger granularity. A lease time of Lease.FOREVER is a request for an indefinite lease; if the service chooses not to grant an indefinite lease, it will return a bounded (non-zero) lease. The final argument to the notify request is a MarshaledObject called the handback.
Each notify returns a net.jini.core.event.EventRegistration object. When an object is written that matches the template supplied in the notify invocation, the listener's notify method is eventually invoked, with a RemoteEvent object.  The RemoteEvent object's getID method will return the same value returned by the EventRegistration object's getID method.  This value, the event ID, will be unique at least with respect to all other active event registrations on this JavaSpaces service with different templates or transactions.  The RemoteEvent object's getSource method will return a reference to the JavaSpaces service.  If the handback associated with the registration was non-null, the RemoteEvent object's getRegistrationObject method will return an object that is equivalent (in the sense of the equals method) to the handback, otherwise this method will return null. The RemoteEvent object's getSequenceNumber method will return the sequence number assigned to this event.  Sequence numbers for a given event ID are strictly increasing. If there is no gap between two sequence numbers, no events have been missed; if there is a gap, events might (but might not) have been missed. For example, a gap might occur if the JavaSpaces service crashes, even if no events are lost due to the crash.
   If the transaction parameter is null, the listener will be notified when matching entries are written either under a null transaction or when a transaction commits. If an entry is written under a transaction and then taken under that same transaction before the transaction is committed, listeners registered under a null transaction will not be notified of that entry.
   If the transaction parameter is not null, the listener will be notified of matching entries written under that transaction in addition to the notifications it would receive under a null transaction. A notify made with a non-null transaction is implicitly dropped when the transaction completes.
   The request specified by a successful notify is as persistent as the entries of the space. They will be remembered as long as an untaken entry would be, until the lease expires, or until any governing transaction completes, whichever is shorter.
   The service will make a "best effort" attempt to deliver notifications. The service will retry at most until the notification request's lease expires. Notifications may be delivered in any order.
   See the Jini Distributed Events Specification for details on the event types.
   JS.2.8	 Operation Ordering
   Operations on a space are unordered. The only view of operation order can be a thread's view of the order of the operations it performs. A view of inter-thread order can be imposed only by cooperating threads that use an application-specific protocol to prevent two or more operations being in progress at a single time on a single JavaSpaces service. Such means are outside the purview of this specification.
   For example, given two threads T and U, if T performs a write operation and U performs a read with a template that would match the written entry, the read may not find the written entry even if the write returns before the read. Only if T and U cooperate to ensure that the write returns before the read commences would the read be ensured the opportunity to find the entry written by T (although it still might not do so because of an intervening take from a third entity).
   JS.2.9	 Serialized Form
<table border="1" bordercolorlight="#FFFFFF" bordercolordark="#000000"
       cellpadding="5" cellspacing="0" summary="serialized form of InternalSpaceException">
    Class
    serialVersionUID
    Serialized Fields
    InternalSpaceException
    -4167507833172939849L
    all public fields
   JS.2.10	 JavaSpace05 
   The JavaSpace05 interface extends the 
  JavaSpace interface to provide methods that allow clients to perform batch 
  operations on the space (which are generally more efficient than performing 
  an equivalent set of singleton operations) and to more easily develop 
  applications where a given Entry needs to be read by multiple clients. 
  Implementations of the JavaSpace interface are not required to implement 
  this interface.
   Unless otherwise noted, the effects of any invocation of a method defined by this interface must be visible to any operation on the space that is started after the invocation returns normally. Note, the effects of a method invocation that throws a RemoteException are not necessarily visible when the exception is thrown. 
   All of the methods of this interface take one or more Collections as arguments. Each such Collection must be treated as immutable by implementations and must not be changed by the client during the course of any method invocation to which they have been passed.
   The JavaSpace05 interface is not a remote interface. Each implementation of this interface exports a proxy object that implements this interface local to the client. Each method of the interface takes as one of its arguments a Collection of Entry instances. The entries themselves must be serialized in accordance with the Jini Entry Specification and will not be altered by the call. Typically, the Collection holding the entries will not be serialized at all. If one of these entries can't be serialized, a MarshalException will be thrown. Aside from the handling of these Collection of Entry parameters, all methods defined by this interface otherwise obey normal Java(TM) Remote Method Invocation remote interface semantics. 
   The JavaSpace05 interface is:
package net.jini.space;
import java.rmi.*;
import java.util.*
import net.jini.core.event.*;
import net.jini.core.transaction.*;
import net.jini.core.lease.*;
import net.jini.entry.*
public interface JavaSpace05 extends JavaSpace {
    public List write(List        entries,
		      Transaction txn,
		      List        leaseDurations)
	throws TransactionException, RemoteException;
    public Collection take(Collection  tmpls, 
			   Transaction txn, 
			   long        timeout, 
			   long        maxEntries)
	throws UnusableEntriesException, TransactionException,
	       RemoteException;
    public MatchSet contents(Collection  tmpls, 
			     Transaction txn,
			     long        leaseDuration, 
			     long        maxEntries)
	throws TransactionException, RemoteException;
    public EventRegistration 
	registerForAvailabilityEvent(Collection          tmpls, 
				     Transaction         txn,
				     boolean             visibilityOnly,
				     RemoteEventListener listener,
				     long                leaseDuration, 
				     MarshalledObject    handback)
	throws TransactionException, RemoteException;
}
   The details of each JavaSpace05 method are given in the sections that follow.
   JS.2.10.1	 MatchSet 
   The MatchSet interface is a collection of 
  Entry instances to be incrementally returned from a JavaSpaces service. 
  Some operations on a space must return more entries than can be conveniently 
  returned by a single call, generally because returning all the entries in one 
  result would consume too many resources in the client or introduce too much 
  latency before the first entry could be processed. In these cases, match sets
  are returned to incrementally return the necessary entries. A MatchSet 
  instance is a proxy object that can be used to access a match set created 
  by a space.
   Typically match sets are created by some factory method 
  on the space (for example, JavaSpace05.contents) which returns a MatchSet 
  instance that will serve as a proxy to the match set. The match set will 
  initially contain some population of entries specified by the operation that 
  created it. These entries can be retrieved by calling next. 
  A successful call to next will remove the returned Entry from the match set. 
  Match sets can end up in one of two terminal states, exhausted or invalidated.
  Under what conditions a match set enters one of these states is specified by 
  the operation that created it. An exhausted match set is empty and will have 
  no more entries added. Calling next on an exhausted match set must always 
  return null. Calling next on an invalidated match set may return a non-null 
  value, or it may throw one of the allowed exceptions. In particular it may 
  throw NoSuchObjectException to indicate that the match set has been 
  invalidated. Once next throws a NoSuchObjectException, all future next 
  calls on that instance must also throw NoSuchObjectException. Calling next 
  on an invalidated match set must never return null. 
  Implementations must not add entries to an invalidated match set.
   Between the time a match set is created and the time it reaches a terminal state, entries may be added by the space. However, an Entry that is removed by a next call must not be added back to a match set (though if there is a distinct but equivalent entry in the space it may be added). The space may also remove entries independent of next calls. The conditions under which entries will be removed independent of next calls or added after the initial creation of the match set are specified by the operation that created the match set. 
   If there is a possibility that a match set may become invalidated, it must be leased and an appropriate proxy must be returned by the getLease method. If there is no possibility that the match set will become invalidated, implementations should not lease the match set. If a match set is not leased, then getLease must return null. 
   An active lease on a match set serves as a hint to the space that the client is still interested in the match set, and as a hint to the client that the match set is still functioning. However, implementations are allowed to invalidate match sets associated with active leases and to unilaterally cancel leases associated with functioning match sets. If a match set is leased and the lease is active, implementations should, to the best of their ability, maintain the match set and not invalidate it. There are cases, however, where this may not be possible in particular, it is not expected that implementations will maintain match sets across crashes. If the lease expires or is canceled, an implementation should invalidate the match set. An implementation must unilaterally cancel a match set's lease if the match set is invalidated. An implementation may unilaterally cancel the lease at other times without necessarily invalidating the match set. Clients should not assume that the resources associated with a leased match set will be freed if the match set reaches the exhausted state, and should instead cancel the lease. This interface is not a remote interface; though in general a MatchSet is a proxy for some remote match set, only the next method is considered to be a remote method, and as outlined in its description it deviates in a number of ways from normal Java(TM) Remote Method Invocation remote method semantics.
package net.jini.space;
public interface MatchSet {
    public Entry next() throws RemoteException, UnusableEntryException;
    public Lease getLease();
    public Entry getSnapshot();
}
   JS.2.10.2	 MatchSet.next 
   Removes one Entry from the match set and returns a copy to the caller. Returns null if the match set is empty. The returned Entry must be unmarshalled in accordance with the Jini Entry Specification.
   A given invocation of this method may perform remote communications, but generally the next method is not expected to have remote method constraints that can vary from invocation to invocation. Instead the set of constraints associated with the next method will be fixed at the time the match set was created, even if this object implements an interface like RemoteMethodControl that would otherwise allow the set of constraints associated with the next method to be altered.
     If an UnusableEntryException is thrown, the Entry removed from the MatchSet could not be unmarshalled.  If a RemoteException is thrown, a communication error has occurred and no entry was removed.
   JS.2.10.3	 MatchSet.getLease 
   Returns a proxy to the Lease associated with this match set, or null if this match set is not leased.
   JS.2.10.4	 MatchSet.getSnapshot 
   Returns a snapshot of the Entry removed by the last call to next. 
   Any snapshot returned by this method will meet the same contract as the object returned by passing the result of the last next invocation to JavaSpace.snapshot.
     If an IllegalStateExecption is thrown, the last call to next did not remove an Entry from the match set, or next was not called.
   JS.2.11	 JavaSpace05.write 
   This method provides an overload of the JavaSpace.write method that allows new copies of multiple Entry instances to be stored in the space using a single call. The client may specify a Transaction for the operation to be performed under. Each Entry to be stored in the space has a separate requested initial lease duration. 
   The effect on the space of an invocation of this method successfully storing an Entry is the same as if the Entry had been successfully stored by a call to the singleton form of write under txn with the given requested initial lease duration. This method returns the proxies to the leases for each newly stored Entry by returning a List of Lease instances. The i th element of the returned List will be a proxy for the lease on the Entry created from the i th element of entries. 
   If an invocation of this method returns normally, then a new copy of each element of entries must have been stored in the space. A new copy of each element will be stored even if there are duplicates (either in terms of object identity or of entry equivalence) in entries. 
   The order in which the entries stored by an invocation of this method will become visible in the space is unspecified, and different observers may see the entries become visible in different orders.
     If a TransactionException, SecurityException, IllegalArgumentException, or NullPointerException is thrown, no entries will have been added to the space by this operation. If a RemoteException is thrown, either new copies of all of the elements of entries will have been stored or no entries will have been stored; that is, in the case of a RemoteException, the storing of new entries in the space will either fail or succeed as a unit. 
   JS.2.12	 JavaSpace05.take 
     This method provides an overload of the JavaSpace.take method that attempts to remove, optionally under a Transaction, and return one or more entries from the space. Each Entry taken will match one or more elements of the passed Collection of templates, and all of the taken entries will be visible to the passed Transaction. If there are initially no matches in the space that are visible to the passed Transaction, an invocation of this method will block for up to a specified timeout for one or more matches to appear. 
     The effect on the space of an invocation of this method successfully taking an Entry will be the same as if the Entry had been taken using the singleton version of this method and passing txn as the Transaction. 
     The tmpls parameter must be a Collection of Entry instances to be used as templates. All of the entries taken must match one or more of these templates. The tmpls parameter may contain null values and may contain duplicates. An Entry is said to be available to an invocation of this method if the Entry could have been returned by an invocation of the singleton take method using txn. 
     If the method succeeds, a non-null Collection will be returned. The Collection will contain a copy of each Entry that was taken. If no entries were taken, the Collection will be empty. Each Entry taken will be represented by a distinct Entry instance in the returned Collection, even if some of the entries are equivalent to others taken by the operation. There will be no null elements in the returned Collection.
     If one or more of the entries taken cannot be unmarshalled in the client, an UnusableEntriesException is thrown. The exception's UnusableEntriesException.getEntries method will return a Collection with a copy of each Entry that could be unmarshalled. The UnusableEntriesException.getUnusableEntryExceptions method will return a Collection with an UnusableEntryException for each Entry that could not be unmarshalled. Every Entry taken by the invocation will either be represented in the Collection returned by getEntries or in the Collection returned by getUnusableEntryExceptions. 
     If there is at least one matching Entry available in the space, an invocation of this method must take at least one Entry. If more than one matching Entry is available, the invocation may take additional entries. It must not take more than maxEntries, but an implementation may chose to take fewer entries from the space than the maximum available or the maximum allowed by maxEntries. If for whatever reason, an invocation of this method takes fewer entries than the maximum number of available matching entries, how an implementation selects which entries should be taken by the invocation and which are left in the space is unspecified. How consumption of entries is arbitrated between conflicting queries is also unspecified. 
     If there are initially no matching entries in the space, an invocation of this method should block for up to timeout milliseconds for a match to appear. If one or more matches become available before timeout expires, one or more of the newly available entries should be taken and the method should return without further blocking. If for some reason the invocation can't block for the full timeout and no entries have been taken, the invocation must fail with a RemoteException or TransactionException as appropriate. 
     If an invocation of this method removes (or locks) more than one Entry, the order in which the removal (or locking) occurs is undefined, and different observers may see the removal or locking of the entries in different orders. 
     If a TransactionException, SecurityException, IllegalArgumentException, or NullPointerException is thrown, no entries will have been taken. If a RemoteException is thrown, up to maxEntries may have been taken by this operation. 
   JS.2.13	 JavaSpace05.contents 
   Creates a match set that can be used to exhaustively read through all of the matching entries in the space that are visible to the passed Transaction and remain visible for the lifetime of the match set. May also yield additional entries that match but are only visible for part of the lifetime of the match set. 
   The tmpls parameter must be a Collection of Entry instances to be used as templates. All of the entries placed in the match set will match one or more of these templates. tmpls may contain null values and may contain duplicates. An Entry is said to be visible to an invocation of this method if the Entry could have been returned by a singleton JavaSpace.read using the same transaction. 
   The resulting match set must initially contain all of the visible matching entries in the space. During the lifetime of the match set an Entry may be, but is not required to be, added to the match set if it becomes visible. If the match set becomes empty, no more entries can be added and the match set enters the exhausted state.
   Normally there are three conditions under which an Entry might be removed from the match set: 
     Any Entry yielded by an invocation of the MatchSet.next method on the match set (either as the return value of a successful call or embedded in an UnusableEntryException) must be removed from the match set. 
     Any Entry that remains in the match set after maxEntries entries are yielded by next invocations must be removed from the match set. In such a case, the criteria used to select which entries are yielded by next calls and which get removed from the set at the end is unspecified. 
     Any Entry that during the lifetime of the match set becomes invisible may at the discretion of the implementation be removed from the match set. 
   An implementation may decide to remove an Entry from the set for other reasons. If it does so, however, it must invalidate the set.
   If txn is non-null and still active, any Entry removed from the match set by a next call must be locked as if it had been returned by a read operation using txn. An implementation may establish the read lock on the Entry any time between when the Entry is added to the match set and when the Entry is removed from the match set by an invocation of next. These read locks are not released when the match set reaches either the exhausted state or the invalidated state. If from the space's perspective the txn leaves the active state, the space must remove from the match set any entries in the match set that have not yet been read locked. This may require the match set to be invalidated. 
   If the match set is leased and leaseDuration is positive, the initial duration of the lease must be less than or equal to leaseDuration. If leaseDuration is Lease.ANY, the initial duration of the lease can be any positive value desired by the implementation. 
   If there are remote method constraints associated with an invocation of this method, any remote communications performed by or on behalf of the match set's next method will be performed in compliance with these constraints, not with the constraints (if any) associated with next. 
   JS.2.14	 JavaSpace05.registerForAvailabilityEvent 
   Register for events triggered when a matching Entry transitions from unavailable to available. The resulting events will be instances of the AvailabilityEvent class and the AvailabilityEvent.getEntry method will return a copy of the Entry whose transition triggered the event.
   An Entry makes a transition from unavailable to available when it goes from being in a state where it could not be returned by a JavaSpace.take using txn to a state where it could be returned. An Entry makes a transition from invisible to visible when it goes from being in a state where it could not be returned by a JavaSpace.read using txn to a state where it could be returned. Note, any transition from invisible to visible is also a transition from unavailable to available, but an already visible entry can be unavailable and then make a transition from unavailable to available. Because the entry was already visible, this transition would not be a transition from invisible to visible.  
   The tmpls parameter must be a Collection of Entry instances to be used as templates. Events will be generated when an Entry that matches one or more of these templates makes an appropriate transition. A single transition will generate only one event per registration, in particular the transition of an Entry that matches multiple elements of tmpls must still generate exactly one event for this registration. If a given Entry undergoes multiple applicable transitions while the registration is active, each must generate a separate event.  
   Events are not generated directly by the transition of matching entries, but instead by an abstract observer set up in the space for each registration. The observer may see the transitions out of order and as a result the order of the events generated for this registration (as determined by the sequence numbers assigned to the events) may be different from the order of the transitions themselves. Additionally, each registration will have its own abstract observer and different observers may see the same sequence of transitions in different orders. As a result, given a set of transitions that trigger events for two different registrations, the order of the events generated for one registration may differ from the order of the events generated for the other.   
   A non-null EventRegistration object will be returned. Each registration will be assigned an event ID. The event ID will be unique at least with respect to all other active event registrations for AvailabilityEvents on this space with a non-equivalent set of templates, a different transaction, and/or a different value for the visibilityOnly flag. The event ID can be obtained by calling the EventRegistration.getID method on the returned EventRegistration. The returned EventRegistration object's EventRegistration.getSource method will return a reference to the space.  
   Registrations are leased. leaseDurations represents the client's desired initial lease duration. If leaseDuration is positive, the initial lease duration will be a positive value less than or equal to leaseDuration. If leaseDuration is Lease.ANY, the space is free to pick any positive initial lease duration it desires. A proxy for the lease associated with the registration can be obtained by calling the returned EventRegistration's EventRegistration.getLease method.  
   A registration made with a non-null value for txn is implicitly dropped when the space observes txn has left the active state.  
   JS.3	 Transactions	 
  The JavaSpaces API uses the package net.jini.core.transaction to provide basic atomic transactions that group multiple operations across multiple JavaSpaces services into a bundle that acts as a single atomic operation. JavaSpaces services are actors in these transactions; the client can be an actor as well, as can any remote object that implements the appropriate interfaces.
   Transactions wrap together multiple operations. Either all modifications within the transactions will be applied or none will, whether the transaction spans one or more operations and/or one or more JavaSpaces services.
   The transaction semantics described here conform to the default transaction semantics defined in the Jini Transaction Specification.
   JS.3.1	 Operations under Transactions
   Any read, write, or take operations that have a null transaction act as if they were in a committed transaction that contained exactly that operation. For example, a take with a null transaction parameter performs as if a transaction was created, the take performed under that transaction, and then the transaction was committed. Any notify operations with a null transaction apply to write operations that are committed to the entire space.
   Transactions affect operations in the following ways:
   write: An entry that is written is not visible outside its transaction until the transaction successfully commits. If the entry is taken within the transaction, the entry will never be visible outside the transaction and will not be added to the space when the transaction commits. Specifically, the entry will not generate notifications to listeners that are not registered under the writing transaction. Entries written under a transaction that aborts are discarded.
   read: A read may match any entry written under that transaction or in the entire space. A JavaSpaces service is not required to prefer matching entries written inside the transaction to those in the entire space. When read, an entry is added to the set of entries read by the provided transaction. Such an entry may be read in any other transaction to which the entry is visible, but cannot be taken in another transaction.
   take: A take matches like a read with the same template. When taken, an entry is added to the set of entries taken by the provided transaction. Such an entry may not be read or taken by any other transaction.
   notify: A notify performed under a null transaction applies to write operations that are committed to the entire space. A notify performed under a non-null transaction additionally provides notification of writes performed within that transaction. When a transaction completes, any registrations under that transaction are implicitly dropped. When a transaction commits, any entries that were written under the transaction (and not taken) will cause appropriate notifications for registrations that were made under a null transaction.
   If a transaction aborts while an operation is in progress under that transaction, the operation will terminate with a TransactionException. Any statement made in this chapter about read or take apply equally to readIfExists or takeIfExists, respectively.
   JS.3.2	 Transactions and ACID Properties
   The ACID properties traditionally offered by database transactions are preserved in transactions on JavaSpaces systems. The ACID properties are:
   Atomicity: All the operations grouped under a transaction occur or none of them do.
   Consistency: The completion of a transaction must leave the system in a consistent state. Consistency includes issues known only to humans, such as that an employee should always have a manager. The enforcement of consistency is outside of the transaction--a transaction is a tool to allow consistency guarantees, and not itself a guarantor of consistency.
   Isolation: Ongoing transactions should not affect each other. Any observer should be able to see other transactions executing in some sequential order (although different observers may see different orders).
   Durability: The results of a transaction should be as persistent as the entity on which the transaction commits.
   The timeout values in read and take allow a client to trade full isolation for liveness. For example, if a read request has only one matching entry and that entry is currently locked in a take from another transaction, read would block indefinitely if the client wanted to preserve isolation. Since completing the transaction could take an indefinite amount of time, a client may choose instead to put an upper bound on how long it is willing to wait for such isolation guarantees, and instead proceed to either abort its own transaction or ask the user whether to continue or whatever else is appropriate for the client.
   Persistence is not a required property of JavaSpaces technology implementations. A transient implementation that does not preserve its contents between system crashes is a proper implementation of the JavaSpace interface's contract, and may be quite useful. If you choose to perform operations on such a space, your transactions will guarantee as much durability as the JavaSpaces service allows for all its data, which is all that any transaction system can guarantee.
   JS.4	 Further Reading	 
   JS.4.1	 Linda Systems
       "How to Write Parallel Programs: A Guide to the Perplexed," Nicholas Carriero and David Gelernter, ACM Computing Surveys, Sept., 1989.
       "Generative Communication in Linda," David Gelernter, ACM Transactions on Programming Languages and Systems, Vol. 7, No. 1, pp. 80-112 (January 1985).
       "Persistent Linda: Linda + Transactions + Query Processing," Brian G. Anderson and Dennis Shasha, Proceedings of the 13th Symposium on Fault-Tolerant Distributed Systems, 1994.
       "Adding Fault-tolerant Transaction Processing to LINDA," Scott R. Cannon and David Dunn, Software--Practice and Experience, Vol. 24(5), pp. 449-446 (May 1994).
       ActorSpaces: An Open Distributed Programming Paradigm, Gul Agha, Christian J. Callsen, University of Illinois at Urbana-Champaign, UILU-ENG-92-1846.
   JS.4.2	 The Java Platform
       The Java Programming Language, Third Edition, Ken Arnold, James Gosling, and David Holmes, Addison Wesley, 2000.
       The Java Language Specification, James Gosling, Bill Joy, and Guy Steele, Addison Wesley, 1996.
       The Java Virtual Machine Specification, Second Edition, Tim Lindholm and Frank Yellin, Addison Wesley, 1999.
       The Java Class Libraries, Second Edition, Patrick Chan, Rosanna Lee, and Doug Kramer, Addison Wesley, 1998.
   JS.4.3	 Distributed Computing
       Distributed Systems, Sape Mullender, Addison Wesley, 1993.
       Distributed Systems: Concepts and Design, George Coulouris, Jean Dollimore, and Tim Kindberg, Addison Wesley, 1998.
       Distributed Algorithms, Nancy A. Lynch, Morgan Kaufmann Publishers, 1997.
   JS.5	 History	 
    Version
    Description
    v1.0
    Initial release of this specification.
    v2.0 (formerly called v1.2)
    Section JS.2.7 was changed to no longer require that event sequence numbers be "fully ordered."4
    v2.1 (formerly called v1.2.1)
    A paragraph in Section JS.2.7 was inadvertently left in the document during the v1.2 revision. That paragraph has been deleted in this release.
    v2.2 
    Removed dependency on Jini Entry Utilities Specification.
    v2.3 
    Added optional JavaSpace05 interface.
1 As used in this document, the terms "Java virtual machine" or "JVM" mean a virtual machine for the Java platform.
2 The term "space" is used to refer to a JavaSpaces service implementation.
 3 "Linda" is the name of a public domain technology originally propounded by Dr. David Gelernter of Yale University. "Linda" is also claimed as a trademark for certain goods by Scientific Computing Associates, Inc. This discussion refers to the public domain "Linda" technology.
 4 "Fully ordered" in defined in the Jini Technology Core Platform Specification, "Distributed Events".
 		 License	 
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership. The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License. You may obtain a copy of the License at
     http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
Spec Index
JavaSpaces Service Specifications
