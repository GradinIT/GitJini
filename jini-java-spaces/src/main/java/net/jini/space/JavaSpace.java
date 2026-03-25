package net.jini.space;

import java.rmi.MarshalledObject;
import java.rmi.RemoteException;
import net.jini.core.entry.Entry;
import net.jini.core.entry.UnusableEntryException;
import net.jini.core.event.EventRegistration;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.lease.Lease;
import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;

/**
 * The JavaSpace interface defines the operations that can be performed on a JavaSpaces service.
 * A JavaSpaces service holds entries. An entry is a typed group of objects, expressed in a
 * class for the Java platform that implements the interface net.jini.core.entry.Entry.
 *
 * All operations on a JavaSpace are performed in a transactionally secure manner.
 */
public interface JavaSpace {
    /**
     * Wait for no time at all.
     */
    long NO_WAIT = 0;

    /**
     * Write the given entry into this JavaSpaces service.
     *
     * @param e the entry to write
     * @param txn the transaction under which to perform the write, or null
     * @param lease the requested lease time in milliseconds
     * @return a lease for the entry
     * @throws RemoteException if a communication error occurs
     * @throws TransactionException if a transaction error occurs
     */
    Lease write(Entry e, Transaction txn, long lease)
        throws RemoteException, TransactionException;

    /**
     * Read an entry from this JavaSpaces service that matches the given template.
     * If more than one entry matches, any one may be returned.
     *
     * @param tmpl the template to match
     * @param txn the transaction under which to perform the read, or null
     * @param timeout how long to wait for a match, in milliseconds
     * @return a copy of the matching entry, or null if no match is found
     * @throws TransactionException if a transaction error occurs
     * @throws UnusableEntryException if the matching entry is unusable
     * @throws RemoteException if a communication error occurs
     * @throws InterruptedException if the operation is interrupted
     */
    Entry read(Entry tmpl, Transaction txn, long timeout)
        throws TransactionException, UnusableEntryException,
               RemoteException, InterruptedException;

    /**
     * Read an entry from this JavaSpaces service that matches the given template.
     * This method acts like read, but it will not wait for a match to appear if
     * none is currently available, although it may wait for transactions to settle.
     *
     * @param tmpl the template to match
     * @param txn the transaction under which to perform the read, or null
     * @param timeout how long to wait for transactions to settle, in milliseconds
     * @return a copy of the matching entry, or null if no match is found
     * @throws TransactionException if a transaction error occurs
     * @throws UnusableEntryException if the matching entry is unusable
     * @throws RemoteException if a communication error occurs
     * @throws InterruptedException if the operation is interrupted
     */
    Entry readIfExists(Entry tmpl, Transaction txn, long timeout)
        throws TransactionException, UnusableEntryException,
               RemoteException, InterruptedException;

    /**
     * Read and remove an entry from this JavaSpaces service that matches the given template.
     *
     * @param tmpl the template to match
     * @param txn the transaction under which to perform the take, or null
     * @param timeout how long to wait for a match, in milliseconds
     * @return the matching entry, or null if no match is found
     * @throws TransactionException if a transaction error occurs
     * @throws UnusableEntryException if the matching entry is unusable
     * @throws RemoteException if a communication error occurs
     * @throws InterruptedException if the operation is interrupted
     */
    Entry take(Entry tmpl, Transaction txn, long timeout)
        throws TransactionException, UnusableEntryException,
               RemoteException, InterruptedException;

    /**
     * Read and remove an entry from this JavaSpaces service that matches the given template.
     * This method acts like take, but it will not wait for a match to appear if
     * none is currently available, although it may wait for transactions to settle.
     *
     * @param tmpl the template to match
     * @param txn the transaction under which to perform the take, or null
     * @param timeout how long to wait for transactions to settle, in milliseconds
     * @return the matching entry, or null if no match is found
     * @throws TransactionException if a transaction error occurs
     * @throws UnusableEntryException if the matching entry is unusable
     * @throws RemoteException if a communication error occurs
     * @throws InterruptedException if the operation is interrupted
     */
    Entry takeIfExists(Entry tmpl, Transaction txn, long timeout)
        throws TransactionException, UnusableEntryException,
               RemoteException, InterruptedException;

    /**
     * Register interest in future incoming entries that match the given template.
     *
     * @param tmpl the template to match
     * @param txn the transaction under which to perform the registration, or null
     * @param listener the listener to notify when a match occurs
     * @param lease the requested lease time in milliseconds
     * @param handback an object to be returned with the event notification
     * @return an event registration
     * @throws RemoteException if a communication error occurs
     * @throws TransactionException if a transaction error occurs
     */
    EventRegistration notify(Entry tmpl, Transaction txn,
              RemoteEventListener listener, long lease,
              MarshalledObject handback)
        throws RemoteException, TransactionException;

    /**
     * Return a snapshot of the given entry. The returned snapshot can be used
     * in place of the original entry in future operations on this space.
     *
     * @param e the entry to snapshot
     * @return a snapshot of the entry
     * @throws RemoteException if a communication error occurs
     */
    Entry snapshot(Entry e) throws RemoteException;
}
