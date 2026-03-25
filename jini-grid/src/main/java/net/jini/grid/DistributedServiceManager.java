package net.jini.grid;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface DistributedServiceManager extends Remote {
    void deploy(ServiceUnit unit) throws RemoteException;
    void undeploy(String unitName) throws RemoteException;
    void redeploy(ServiceUnit unit) throws RemoteException;
    boolean isAlive() throws RemoteException;
}
