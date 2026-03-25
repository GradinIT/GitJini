package net.jini.space;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.rmi.MarshalledObject;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.jini.core.entry.Entry;
import net.jini.core.entry.UnusableEntryException;
import net.jini.core.event.EventRegistration;
import net.jini.core.event.RemoteEvent;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.lease.Lease;
import net.jini.core.lease.LeaseException;
import net.jini.core.lease.UnknownLeaseException;
import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import net.jini.entry.UnusableEntriesException;
import java.rmi.MarshalledObject;

/**
 * A basic in-memory implementation of the JavaSpace interface.
 * Note: This implementation is for demonstration and local use.
 */
public class BasicJavaSpace implements JavaSpace05 {

    private final List<SpaceEntry> entries = new CopyOnWriteArrayList<>();
    private final List<Registration> notifications = new CopyOnWriteArrayList<>();
    private final Map<Transaction, TransactionState> transactions = new ConcurrentHashMap<>();
    public interface PersistenceStore {
        void write(UUID id, Entry entry, long expiration);
        void remove(UUID id);
        Map<UUID, StoredEntry> loadAll();
    }

    public static class StoredEntry {
        public final Entry entry;
        public final long expiration;

        public StoredEntry(Entry entry, long expiration) {
            this.entry = entry;
            this.expiration = expiration;
        }
    }

    private PersistenceStore store;
    private long eventSequence = 0;

    public void setPersistenceStore(PersistenceStore store) {
        this.store = store;
        if (store != null) {
            Map<UUID, StoredEntry> loaded = store.loadAll();
            for (Map.Entry<UUID, StoredEntry> e : loaded.entrySet()) {
                SpaceEntry se = new SpaceEntry(e.getValue().entry, e.getValue().expiration);
                // We should ideally preserve the UUID but SpaceEntry generates a new one.
                // For this basic impl it's fine.
                entries.add(se);
            }
        }
    }

    private static class SpaceEntry {
        final Entry entry;
        volatile long expiration;
        final UUID id;
        Transaction lock;

        SpaceEntry(Entry entry, long expiration) {
            this.entry = entry;
            this.expiration = expiration;
            this.id = UUID.randomUUID();
        }

        boolean isExpired() {
            return expiration < System.currentTimeMillis();
        }
    }

    private static class TransactionState {
        final List<SpaceEntry> writes = new ArrayList<>();
        final List<SpaceEntry> takes = new ArrayList<>();
    }

    private static class Registration {
        final Entry tmpl;
        final RemoteEventListener listener;
        volatile long expiration;
        final long eventID;
        final MarshalledObject handback;

        Registration(Entry tmpl, RemoteEventListener listener, long expiration, long eventID, MarshalledObject handback) {
            this.tmpl = tmpl;
            this.listener = listener;
            this.expiration = expiration;
            this.eventID = eventID;
            this.handback = handback;
        }

        boolean isExpired() {
            return expiration < System.currentTimeMillis();
        }
    }

    @Override
    public Lease write(Entry e, Transaction txn, long leaseDuration)
            throws RemoteException, TransactionException {
        if (e == null) {
            throw new IllegalArgumentException("Cannot write null entry");
        }
        
        long expiration = System.currentTimeMillis() + (leaseDuration > 0 ? leaseDuration : 3600000);
        SpaceEntry se = new SpaceEntry(e, expiration);
        
        if (txn == null) {
            entries.add(se);
            if (store != null) store.write(se.id, e, expiration);
            checkNotifications(e);
        } else {
            TransactionState state = transactions.computeIfAbsent(txn, k -> new TransactionState());
            state.writes.add(se);
            se.lock = txn;
        }
        
        return new SpaceLease(se, this);
    }

    private void checkNotifications(Entry e) {
        long seq;
        synchronized (this) {
            seq = ++eventSequence;
        }
        for (Registration reg : notifications) {
            if (reg.isExpired()) {
                notifications.remove(reg);
                continue;
            }
            if (matches(reg.tmpl, e)) {
                try {
                    reg.listener.notify(new RemoteEvent(this, reg.eventID, seq, (Serializable) reg.handback));
                } catch (Exception ex) {
                    // Ignore or log
                }
            }
        }
    }

    @Override
    public Entry read(Entry tmpl, Transaction txn, long timeout)
            throws TransactionException, UnusableEntryException,
                   RemoteException, InterruptedException {
        return find(tmpl, txn, timeout, false, false);
    }

    @Override
    public Entry readIfExists(Entry tmpl, Transaction txn, long timeout)
            throws TransactionException, UnusableEntryException,
                   RemoteException, InterruptedException {
        return find(tmpl, txn, timeout, false, true);
    }

    @Override
    public Entry take(Entry tmpl, Transaction txn, long timeout)
            throws TransactionException, UnusableEntryException,
                   RemoteException, InterruptedException {
        return find(tmpl, txn, timeout, true, false);
    }

    @Override
    public Entry takeIfExists(Entry tmpl, Transaction txn, long timeout)
            throws TransactionException, UnusableEntryException,
                   RemoteException, InterruptedException {
        return find(tmpl, txn, timeout, true, true);
    }

    private Entry find(Entry tmpl, Transaction txn, long timeout, boolean remove, boolean ifExists)
            throws InterruptedException {
        long startTime = System.currentTimeMillis();
        do {
            // Check entries in the space
            for (SpaceEntry se : entries) {
                if (se.isExpired()) {
                    entries.remove(se);
                    continue;
                }
                if (matches(tmpl, se.entry)) {
                    if (se.lock != null && se.lock != txn) {
                        continue; // Locked by another transaction
                    }
                    if (remove) {
                        if (txn == null) {
                            if (entries.remove(se)) {
                                if (store != null) store.remove(se.id);
                                return se.entry;
                            }
                        } else {
                            if (entries.remove(se)) {
                                TransactionState state = transactions.computeIfAbsent(txn, k -> new TransactionState());
                                state.takes.add(se);
                                se.lock = txn;
                                return se.entry;
                            }
                        }
                    } else {
                        return se.entry;
                    }
                }
            }

            // Check entries written in this transaction
            if (txn != null) {
                TransactionState state = transactions.get(txn);
                if (state != null) {
                    for (SpaceEntry se : state.writes) {
                        if (matches(tmpl, se.entry)) {
                            if (remove) {
                                state.writes.remove(se);
                                return se.entry;
                            } else {
                                return se.entry;
                            }
                        }
                    }
                }
            }

            if (ifExists) break;

            long remaining = timeout - (System.currentTimeMillis() - startTime);
            if (remaining <= 0) break;
            Thread.sleep(Math.min(remaining, 100));
        } while (true);

        return null;
    }

    private boolean matches(Entry tmpl, Entry entry) {
        if (tmpl == null) return true;
        if (!tmpl.getClass().isAssignableFrom(entry.getClass())) return false;

        for (Field f : tmpl.getClass().getFields()) {
            if (Modifier.isStatic(f.getModifiers()) || Modifier.isTransient(f.getModifiers())) {
                continue;
            }
            try {
                Object tmplValue = f.get(tmpl);
                if (tmplValue == null) continue;

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
                                   RemoteEventListener listener, long leaseDuration,
                                   MarshalledObject handback)
            throws RemoteException, TransactionException {
        
        long expiration = System.currentTimeMillis() + (leaseDuration > 0 ? leaseDuration : 3600000);
        long eventID = UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
        
        Registration reg = new Registration(tmpl, listener, expiration, eventID, handback);
        notifications.add(reg);
        
        return new EventRegistration(eventID, this, new NotificationLease(reg, this), eventSequence);
    }

    @Override
    public Entry snapshot(Entry e) throws RemoteException {
        return e;
    }

    // These would be called by a TransactionManager in a real Jini environment.
    // Here we might need a way to trigger them for testing.
    public void commit(Transaction txn) {
        TransactionState state = transactions.remove(txn);
        if (state != null) {
            for (SpaceEntry se : state.writes) {
                se.lock = null;
                entries.add(se);
                if (store != null) store.write(se.id, se.entry, se.expiration);
                checkNotifications(se.entry);
            }
            for (SpaceEntry se : state.takes) {
                se.lock = null;
                if (store != null) store.remove(se.id);
            }
        }
    }

    public void abort(Transaction txn) {
        TransactionState state = transactions.remove(txn);
        if (state != null) {
            for (SpaceEntry se : state.writes) {
                se.lock = null;
            }
            for (SpaceEntry se : state.takes) {
                se.lock = null;
                entries.add(se);
            }
        }
    }

    @Override
    public List write(List entries, Transaction txn, List leaseDurations)
            throws TransactionException, RemoteException {
        if (entries == null) throw new NullPointerException("entries is null");
        if (leaseDurations == null) throw new NullPointerException("leaseDurations is null");
        if (entries.size() != leaseDurations.size()) throw new IllegalArgumentException("entries and leaseDurations must be same length");
        if (entries.isEmpty()) throw new IllegalArgumentException("entries is empty");

        List leases = new ArrayList();
        for (int i = 0; i < entries.size(); i++) {
            Entry e = (Entry) entries.get(i);
            if (e == null) throw new NullPointerException("Entry at " + i + " is null");
            Long d = (Long) leaseDurations.get(i);
            if (d == null) throw new NullPointerException("Lease duration at " + i + " is null");
            leases.add(write(e, txn, d));
        }
        return leases;
    }

    @Override
    public Collection take(Collection templates, Transaction txn, long timeout, long maxEntries)
            throws TransactionException, UnusableEntriesException, RemoteException, InterruptedException {
        if (templates == null) throw new NullPointerException("templates is null");
        if (templates.isEmpty()) throw new IllegalArgumentException("templates is empty");
        
        List result = new ArrayList();
        long startTime = System.currentTimeMillis();
        while (result.size() < maxEntries) {
            Entry found = null;
            for (Object t : templates) {
                if (t != null && !(t instanceof Entry)) throw new IllegalArgumentException("Template must be an Entry");
                found = find((Entry) t, txn, 0, true, true);
                if (found != null) {
                    result.add(found);
                    if (result.size() >= maxEntries) break;
                }
            }
            if (found == null) {
                long now = System.currentTimeMillis();
                if (timeout == 0) break;
                long remaining = timeout - (now - startTime);
                if (remaining <= 0) break;
                Thread.sleep(Math.min(remaining, 100));
            }
        }
        return result;
    }

    @Override
    public MatchSet contents(Collection templates, Transaction txn, long leaseDuration, long maxEntries)
            throws TransactionException, RemoteException {
        if (templates == null) throw new NullPointerException("templates is null");
        List matches = new ArrayList();
        for (SpaceEntry se : entries) {
            if (se.isExpired()) continue;
            for (Object t : templates) {
                if (matches((Entry) t, se.entry)) {
                    matches.add(se.entry);
                    break;
                }
            }
        }
        return new BasicMatchSet(matches);
    }

    private static class BasicMatchSet implements MatchSet {
        private final List entries;
        private int index = 0;
        private Entry last;

        BasicMatchSet(List entries) {
            this.entries = entries;
        }

        @Override
        public Entry next() {
            if (index < entries.size()) {
                last = (Entry) entries.get(index++);
                return last;
            }
            return null;
        }

        @Override
        public Lease getLease() {
            return null; // Simplified for now
        }

        @Override
        public Entry getSnapshot() {
            return last;
        }
    }

    @Override
    public EventRegistration registerForAvailabilityEvent(Collection templates,
                                                         Transaction txn,
                                                         boolean visibilityOnly,
                                                         RemoteEventListener listener,
                                                         long leaseDuration,
                                                         MarshalledObject handback)
            throws TransactionException, RemoteException {
        // Basic implementation treats availability same as notify for now
        // In a full implementation, visibilityOnly would matter (event only when it becomes visible vs exists)
        long expiration = System.currentTimeMillis() + (leaseDuration > 0 ? leaseDuration : 3600000);
        long eventID = UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
        
        for (Object t : templates) {
            Registration reg = new Registration((Entry) t, listener, expiration, eventID, handback);
            notifications.add(reg);
        }
        
        return new EventRegistration(eventID, this, new NotificationLease(new Registration(null, listener, expiration, eventID, handback), this), eventSequence);
    }

    private static class SpaceLease implements Lease {
        private final SpaceEntry entry;
        private final BasicJavaSpace space;

        SpaceLease(SpaceEntry entry, BasicJavaSpace space) {
            this.entry = entry;
            this.space = space;
        }

        @Override
        public long getExpiration() {
            return entry.expiration;
        }

        @Override
        public void renew(long duration) throws LeaseException {
            entry.expiration = System.currentTimeMillis() + duration;
        }

        @Override
        public void cancel() throws UnknownLeaseException {
            space.entries.remove(entry);
        }
    }

    private static class NotificationLease implements Lease {
        private final Registration reg;
        private final BasicJavaSpace space;

        NotificationLease(Registration reg, BasicJavaSpace space) {
            this.reg = reg;
            this.space = space;
        }

        @Override
        public long getExpiration() {
            return reg.expiration;
        }

        @Override
        public void renew(long duration) {
            reg.expiration = System.currentTimeMillis() + duration;
        }

        @Override
        public void cancel() {
            space.notifications.remove(reg);
        }
    }
}
