package net.jini.space;

import java.rmi.RemoteException;
import net.jini.core.entry.Entry;
import net.jini.core.entry.UnusableEntryException;
import net.jini.core.lease.Lease;

/**
 * MatchSet is an interface returned by the contents method of JavaSpace05.
 * It provides a way to read a set of matching entries.
 */
public interface MatchSet {
    Entry next() throws RemoteException, UnusableEntryException;
    Lease getLease();
    Entry getSnapshot();
}
