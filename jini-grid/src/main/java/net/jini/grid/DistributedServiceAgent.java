package net.jini.grid;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface DistributedServiceAgent extends Remote {
    void startDSM() throws RemoteException;
    void startDSC() throws RemoteException;
    void startLUS() throws RemoteException;
    boolean isAlive() throws RemoteException;
}
