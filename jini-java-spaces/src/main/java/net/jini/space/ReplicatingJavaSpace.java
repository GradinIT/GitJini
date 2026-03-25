package net.jini.space;

import net.jini.core.entry.Entry;
import net.jini.core.entry.UnusableEntryException;
import net.jini.core.event.EventRegistration;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.lease.Lease;
import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import net.jini.entry.UnusableEntriesException;

import java.rmi.MarshalledObject;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.List;

/**
 * ReplicatingJavaSpace provides high availability by replicating operations
 * from a primary space to a backup space.
 */
public class ReplicatingJavaSpace implements JavaSpace05, Remote {

    private final JavaSpace05 primary;
    private final JavaSpace05 backup;

    /**
     * Creates a new ReplicatingJavaSpace.
     *
     * @param primary the primary space
     * @param backup the backup space where operations are replicated
     */
    public ReplicatingJavaSpace(JavaSpace05 primary, JavaSpace05 backup) {
        this.primary = primary;
        this.backup = backup;
    }

    @Override
    public Lease write(Entry e, Transaction txn, long lease) throws RemoteException, TransactionException {
        Lease primaryLease = primary.write(e, txn, lease);
        try {
            backup.write(e, txn, lease);
        } catch (Exception ex) {
            // Log replication failure but proceed as primary succeeded
            System.err.println("[Replication] Failed to replicate write to backup: " + ex.getMessage());
        }
        return primaryLease;
    }

    @Override
    public Entry read(Entry tmpl, Transaction txn, long timeout) throws TransactionException, UnusableEntryException, RemoteException, InterruptedException {
        try {
            return primary.read(tmpl, txn, timeout);
        } catch (RemoteException e) {
            if (backup != null) {
                return backup.read(tmpl, txn, timeout);
            }
            throw e;
        }
    }

    @Override
    public Entry readIfExists(Entry tmpl, Transaction txn, long timeout) throws TransactionException, UnusableEntryException, RemoteException, InterruptedException {
        try {
            return primary.readIfExists(tmpl, txn, timeout);
        } catch (RemoteException e) {
            if (backup != null) {
                return backup.readIfExists(tmpl, txn, timeout);
            }
            throw e;
        }
    }

    @Override
    public Entry take(Entry tmpl, Transaction txn, long timeout) throws TransactionException, UnusableEntryException, RemoteException, InterruptedException {
        Entry result = primary.take(tmpl, txn, timeout);
        if (result != null) {
            try {
                // To be fully consistent, we should take from backup too.
                // However, without a shared ID, we might take the wrong entry if multiple match.
                // For a simple HA implementation, we try to keep them in sync.
                backup.take(tmpl, txn, timeout);
            } catch (Exception ex) {
                System.err.println("[Replication] Failed to replicate take to backup: " + ex.getMessage());
            }
        }
        return result;
    }

    @Override
    public Entry takeIfExists(Entry tmpl, Transaction txn, long timeout) throws TransactionException, UnusableEntryException, RemoteException, InterruptedException {
        Entry result = primary.takeIfExists(tmpl, txn, timeout);
        if (result != null) {
            try {
                backup.takeIfExists(tmpl, txn, timeout);
            } catch (Exception ex) {
                System.err.println("[Replication] Failed to replicate takeIfExists to backup: " + ex.getMessage());
            }
        }
        return result;
    }

    @Override
    public EventRegistration notify(Entry tmpl, Transaction txn, RemoteEventListener listener, long lease, MarshalledObject handback) throws RemoteException, TransactionException {
        // Notifications are tricky. Usually only primary should notify.
        // But for HA, if primary fails, backup should have the registration.
        EventRegistration reg = primary.notify(tmpl, txn, listener, lease, handback);
        try {
            backup.notify(tmpl, txn, listener, lease, handback);
        } catch (Exception ex) {
             System.err.println("[Replication] Failed to replicate notify to backup: " + ex.getMessage());
        }
        return reg;
    }

    @Override
    public Entry snapshot(Entry e) throws RemoteException {
        return primary.snapshot(e);
    }

    @Override
    public List write(List entries, Transaction txn, List leaseDurations) throws TransactionException, RemoteException {
        List leases = primary.write(entries, txn, leaseDurations);
        try {
            backup.write(entries, txn, leaseDurations);
        } catch (Exception ex) {
            System.err.println("[Replication] Failed to replicate batch write to backup: " + ex.getMessage());
        }
        return leases;
    }

    @Override
    public Collection take(Collection templates, Transaction txn, long timeout, long maxEntries) throws TransactionException, UnusableEntriesException, RemoteException, InterruptedException {
        Collection result = primary.take(templates, txn, timeout, maxEntries);
        if (result != null && !result.isEmpty()) {
            try {
                // Best effort sync for batch take
                backup.take(templates, txn, timeout, maxEntries);
            } catch (Exception ex) {
                System.err.println("[Replication] Failed to replicate batch take to backup: " + ex.getMessage());
            }
        }
        return result;
    }

    @Override
    public MatchSet contents(Collection templates, Transaction txn, long leaseDuration, long maxEntries) throws TransactionException, RemoteException {
        try {
            return primary.contents(templates, txn, leaseDuration, maxEntries);
        } catch (RemoteException e) {
            if (backup != null) {
                return backup.contents(templates, txn, leaseDuration, maxEntries);
            }
            throw e;
        }
    }

    @Override
    public EventRegistration registerForAvailabilityEvent(Collection templates, Transaction txn, boolean visibilityOnly, RemoteEventListener listener, long leaseDuration, MarshalledObject handback) throws TransactionException, RemoteException {
        EventRegistration reg = primary.registerForAvailabilityEvent(templates, txn, visibilityOnly, listener, leaseDuration, handback);
        try {
            backup.registerForAvailabilityEvent(templates, txn, visibilityOnly, listener, leaseDuration, handback);
        } catch (Exception ex) {
            System.err.println("[Replication] Failed to replicate registerForAvailabilityEvent to backup: " + ex.getMessage());
        }
        return reg;
    }
}
