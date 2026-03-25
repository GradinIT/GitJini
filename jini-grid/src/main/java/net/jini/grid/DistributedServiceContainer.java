package net.jini.grid;

import java.rmi.Remote;
import java.rmi.RemoteException;

import java.util.List;

public interface DistributedServiceContainer extends Remote {
    void deploy(ServiceUnit unit) throws RemoteException;
    void undeploy(String unitName) throws RemoteException;
    List<String> getDeployedUnits() throws RemoteException;
    boolean isAlive() throws RemoteException;
}
