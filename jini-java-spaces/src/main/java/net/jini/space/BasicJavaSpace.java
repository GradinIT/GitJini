package net.jini.space;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.rmi.MarshalledObject;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.jini.core.entry.Entry;
import net.jini.core.entry.UnusableEntryException;
import net.jini.core.event.EventRegistration;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.lease.Lease;
import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;

/**
 * A basic in-memory implementation of the JavaSpace interface.
 * Note: This implementation is for demonstration and local use.
 * It does not support transactions or persistent storage yet.
 */
public class BasicJavaSpace implements JavaSpace {

    private final List<Entry> entries = new CopyOnWriteArrayList<>();

    @Override
    public Lease write(Entry e, Transaction txn, long lease)
            throws RemoteException, TransactionException {
        if (e == null) {
            throw new IllegalArgumentException("Cannot write null entry");
        }
        if (txn != null) {
            throw new UnsupportedOperationException("Transactions not supported yet");
        }
        
        // In a real implementation, we would create a deep copy/serialized version
        entries.add(e);
        
        // Return a dummy lease for now
        return new DummyLease(System.currentTimeMillis() + (lease > 0 ? lease : 3600000));
    }

    @Override
    public Entry read(Entry tmpl, Transaction txn, long timeout)
            throws TransactionException, UnusableEntryException,
                   RemoteException, InterruptedException {
        return find(tmpl, txn, timeout, false);
    }

    @Override
    public Entry readIfExists(Entry tmpl, Transaction txn, long timeout)
            throws TransactionException, UnusableEntryException,
                   RemoteException, InterruptedException {
        return find(tmpl, txn, timeout, false);
    }

    @Override
    public Entry take(Entry tmpl, Transaction txn, long timeout)
            throws TransactionException, UnusableEntryException,
                   RemoteException, InterruptedException {
        return find(tmpl, txn, timeout, true);
    }

    @Override
    public Entry takeIfExists(Entry tmpl, Transaction txn, long timeout)
            throws TransactionException, UnusableEntryException,
                   RemoteException, InterruptedException {
        return find(tmpl, txn, timeout, true);
    }

    private Entry find(Entry tmpl, Transaction txn, long timeout, boolean remove)
            throws InterruptedException {
        if (txn != null) {
            throw new UnsupportedOperationException("Transactions not supported yet");
        }

        long startTime = System.currentTimeMillis();
        do {
            for (Entry e : entries) {
                if (matches(tmpl, e)) {
                    if (remove) {
                        if (entries.remove(e)) {
                            return e;
                        }
                    } else {
                        return e;
                    }
                }
            }
            if (timeout > 0) {
                Thread.sleep(Math.min(timeout, 100));
            }
        } while (System.currentTimeMillis() - startTime < timeout);

        return null;
    }

    private boolean matches(Entry tmpl, Entry entry) {
        if (tmpl == null) return true; // null template matches any entry
        if (!tmpl.getClass().isAssignableFrom(entry.getClass())) return false;

        for (Field f : tmpl.getClass().getFields()) {
            if (Modifier.isStatic(f.getModifiers()) || Modifier.isTransient(f.getModifiers())) {
                continue;
            }
            try {
                Object tmplValue = f.get(tmpl);
                if (tmplValue == null) continue; // wildcard

                Object entryValue = f.get(entry);
                if (!tmplValue.equals(entryValue)) return false;
            } catch (IllegalAccessException ex) {
                throw new InternalSpaceException("Error matching entry", ex);
            }
        }
        return true;
    }

    @Override
    public EventRegistration notify(Entry tmpl, Transaction txn,
                                   RemoteEventListener listener, long lease,
                                   MarshalledObject handback)
            throws RemoteException, TransactionException {
        throw new UnsupportedOperationException("Notifications not supported yet");
    }

    @Override
    public Entry snapshot(Entry e) throws RemoteException {
        return e; // In this basic implementation, snapshot is identity
    }

    private static class DummyLease implements Lease {
        private final long expiration;

        DummyLease(long expiration) {
            this.expiration = expiration;
        }

        @Override
        public long getExpiration() {
            return expiration;
        }

        @Override
        public void renew(long duration) {}

        @Override
        public void cancel() {}
    }
}
