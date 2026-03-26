package net.jini.space;

import net.jini.core.entry.Entry;
import net.jini.core.entry.UnusableEntryException;
import net.jini.core.event.EventRegistration;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.lease.Lease;
import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import net.jini.entry.UnusableEntriesException;
import net.jini.core.entry.RoutingEntry;

import java.lang.reflect.Field;
import java.rmi.MarshalledObject;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * PartitionedJavaSpace routes operations to specific partitions based on routing information.
 */
public class PartitionedJavaSpace implements JavaSpace05, Remote {

    private final JavaSpace05[] partitions;

    public PartitionedJavaSpace(JavaSpace05[] partitions) {
        this.partitions = partitions;
    }

    private JavaSpace05 getPartition(Entry e) {
        if (e == null) return partitions[0];
        Object routingValue = getRoutingValue(e);
        if (routingValue == null) {
            System.out.println("[PARTITIONED] No routing value found for entry, defaulting to partition 0");
            return partitions[0];
        }
        int hash = Math.abs(routingValue.hashCode());
        int partitionIndex = hash % partitions.length;
        System.out.println("[PARTITIONED] Routing entry with value " + routingValue + " to partition " + partitionIndex);
        return partitions[partitionIndex];
    }

    private Object getRoutingValue(Entry e) {
        if (e == null) return null;
        try {
            // Check for @Routing field first (simplified here as looking for field named 'id' or 'routingValue')
            Class<?> current = e.getClass();
            while (current != null && current != Object.class) {
                for (Field f : current.getDeclaredFields()) {
                    String name = f.getName();
                    if ("id".equals(name) || "routingValue".equals(name) || "routingKey".equals(name)) {
                        f.setAccessible(true);
                        Object val = f.get(e);
                        // System.out.println("[PARTITIONED] Found routing field " + name + " in class " + current.getSimpleName() + " with value " + val);
                        return val;
                    }
                }
                current = current.getSuperclass();
            }
        } catch (Exception e2) {
            System.err.println("[PARTITIONED] Error getting routing value: " + e2.getMessage());
        }
        
        // Final fallback: try to find any String field
        try {
             for (Field f : e.getClass().getDeclaredFields()) {
                 if (f.getType() == String.class) {
                     f.setAccessible(true);
                     Object val = f.get(e);
                     if (val != null) return val;
                 }
             }
        } catch (Exception ignored) {}

        return null;
    }

    @Override
    public Lease write(Entry e, Transaction txn, long lease) throws RemoteException, TransactionException {
        return getPartition(e).write(e, txn, lease);
    }

    @Override
    public Entry read(Entry tmpl, Transaction txn, long timeout) throws TransactionException, UnusableEntryException, RemoteException, InterruptedException {
        // In a real system, if routing value is missing, we might broadcast.
        // For simplicity, if routing value exists, go to partition, else broadcast.
        Object routingValue = getRoutingValue(tmpl);
        if (routingValue != null) {
            return getPartition(tmpl).read(tmpl, txn, timeout);
        }
        for (JavaSpace05 partition : partitions) {
            Entry result = partition.read(tmpl, txn, 0); // Use 0 timeout for broadcast
            if (result != null) return result;
        }
        return null;
    }

    @Override
    public Entry readIfExists(Entry tmpl, Transaction txn, long timeout) throws TransactionException, UnusableEntryException, RemoteException, InterruptedException {
        Object routingValue = getRoutingValue(tmpl);
        if (routingValue != null) {
            return getPartition(tmpl).readIfExists(tmpl, txn, timeout);
        }
        for (JavaSpace05 partition : partitions) {
            Entry result = partition.readIfExists(tmpl, txn, 0);
            if (result != null) return result;
        }
        return null;
    }

    @Override
    public Entry take(Entry tmpl, Transaction txn, long timeout) throws TransactionException, UnusableEntryException, RemoteException, InterruptedException {
        Object routingValue = getRoutingValue(tmpl);
        if (routingValue != null) {
            return getPartition(tmpl).take(tmpl, txn, timeout);
        }
        for (JavaSpace05 partition : partitions) {
            Entry result = partition.take(tmpl, txn, 0);
            if (result != null) return result;
        }
        return null;
    }

    @Override
    public Entry takeIfExists(Entry tmpl, Transaction txn, long timeout) throws TransactionException, UnusableEntryException, RemoteException, InterruptedException {
        Object routingValue = getRoutingValue(tmpl);
        if (routingValue != null) {
            return getPartition(tmpl).takeIfExists(tmpl, txn, timeout);
        }
        for (JavaSpace05 partition : partitions) {
            Entry result = partition.takeIfExists(tmpl, txn, 0);
            if (result != null) return result;
        }
        return null;
    }

    @Override
    public EventRegistration notify(Entry tmpl, Transaction txn, RemoteEventListener listener, long lease, MarshalledObject handback) throws RemoteException, TransactionException {
        // Simplified: notify all partitions
        EventRegistration reg = null;
        for (JavaSpace05 partition : partitions) {
            reg = partition.notify(tmpl, txn, listener, lease, handback);
        }
        return reg;
    }

    @Override
    public Entry snapshot(Entry e) throws RemoteException {
        return getPartition(e).snapshot(e);
    }

    @Override
    public List write(List entries, Transaction txn, List leaseDurations) throws TransactionException, RemoteException {
        List results = new ArrayList();
        for (int i = 0; i < entries.size(); i++) {
            Entry e = (Entry) entries.get(i);
            long lease = (Long) leaseDurations.get(i);
            results.add(write(e, txn, lease));
        }
        return results;
    }

    @Override
    public Collection take(Collection templates, Transaction txn, long timeout, long maxEntries) throws TransactionException, UnusableEntriesException, RemoteException, InterruptedException {
        List results = new ArrayList();
        try {
            for (Object tmpl : templates) {
                Entry result = take((Entry) tmpl, txn, timeout);
                if (result != null) {
                    results.add(result);
                    if (results.size() >= maxEntries) break;
                }
            }
        } catch (UnusableEntryException e) {
            List exceptions = new ArrayList();
            exceptions.add(e);
            throw new UnusableEntriesException("Failed to take entries", results, exceptions);
        }
        return results;
    }

    @Override
    public MatchSet contents(Collection templates, Transaction txn, long leaseDuration, long maxEntries) throws TransactionException, RemoteException {
        // Not implemented for partitioned in this simplified version
        throw new RemoteException("Not implemented for PartitionedJavaSpace");
    }

    @Override
    public EventRegistration registerForAvailabilityEvent(Collection templates, Transaction txn, boolean visibilityOnly, RemoteEventListener listener, long leaseDuration, MarshalledObject handback) throws TransactionException, RemoteException {
        EventRegistration reg = null;
        for (JavaSpace05 partition : partitions) {
            reg = partition.registerForAvailabilityEvent(templates, txn, visibilityOnly, listener, leaseDuration, handback);
        }
        return reg;
    }
}
