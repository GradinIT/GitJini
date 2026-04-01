package net.jini.example;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface HelloService extends java.rmi.Remote, java.io.Serializable {
    String sayHello(HelloRequest request) throws RemoteException;
}
