package net.jini.space;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.List;
import net.jini.core.entry.Entry;
import net.jini.core.entry.UnusableEntryException;
import net.jini.core.event.EventRegistration;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.lease.Lease;
import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import net.jini.entry.UnusableEntriesException;

/**
 * The JavaSpace05 interface extends the JavaSpace interface to provide methods that allow
 * clients to perform batch operations on the space and to more easily develop applications
 * where a given Entry needs to be read by multiple clients.
 */
public interface JavaSpace05 extends JavaSpace {
    
    List write(List entries, Transaction txn, List leaseDurations)
        throws TransactionException, RemoteException;

    Collection take(Collection templates, Transaction txn, long timeout, long maxEntries)
        throws TransactionException, UnusableEntriesException, RemoteException, InterruptedException;

    MatchSet contents(Collection templates, Transaction txn, long leaseDuration, long maxEntries)
        throws TransactionException, RemoteException;

    EventRegistration registerForAvailabilityEvent(Collection templates,
                                                  Transaction txn,
                                                  boolean visibilityOnly,
                                                  RemoteEventListener listener,
                                                  long leaseDuration,
                                                  java.rmi.MarshalledObject handback)
        throws TransactionException, RemoteException;
}
