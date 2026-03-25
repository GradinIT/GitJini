package net.jini.example;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface HelloService extends Remote {
    String sayHello(HelloRequest request) throws RemoteException;
}
