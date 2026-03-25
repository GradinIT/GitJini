package net.jini.core.event;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.EventListener;

/**
 * RemoteEventListener is the interface for receiving distributed events.
 */
public interface RemoteEventListener extends Remote, EventListener {
    void notify(RemoteEvent theEvent) throws RemoteException, UnknownEventException;
}
