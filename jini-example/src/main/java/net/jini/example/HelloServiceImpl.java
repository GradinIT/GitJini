package net.jini.example;

import net.jini.core.entry.Entry;
import net.jini.core.export.ExportedService;
import net.jini.core.export.ServiceRouting;
import net.jini.core.lease.Lease;
import net.jini.core.transaction.TransactionException;
import net.jini.space.BasicJavaSpace;

import java.rmi.RemoteException;
import java.io.Serializable;

@ExportedService(id = "hello-service")
public class HelloServiceImpl implements HelloService, Serializable {
    private final String instanceName;
    private final BasicJavaSpace space = new BasicJavaSpace();

    public HelloServiceImpl() {
        this.instanceName = "Default";
    }

    public HelloServiceImpl(String instanceName) {
        this.instanceName = instanceName;
    }

    @Override
    public String sayHello(HelloRequest request) throws RemoteException {
        System.out.println("[SERVICE " + instanceName + "] Received request from: " + request.name + " (Routing Key: " + request.routingKey + ")");
        try {
            space.write(new MyEntry(request.routingKey),null, Lease.FOREVER);
        } catch (TransactionException e) {
            throw new RemoteException(e.getMessage(),e);
        }
        return "Hello " + request.name + "! This is " + instanceName + " Jini service!";
    }
    private class MyEntry implements Entry {
        private final String routingKey;
        public MyEntry(String routingKey) {
            this.routingKey = routingKey;
        }
        @Override
        public String toString() {
            return "RoutingEntry{routingKey='" + routingKey + "'}";
        }
    }
}
