package net.jini.example;

import net.jini.core.export.ExportedService;
import net.jini.core.export.ServiceRouting;
import java.rmi.RemoteException;
import java.io.Serializable;

@ExportedService(id = "hello-service", instanceId = "instance-1")
public class HelloServiceImpl implements HelloService, Serializable {
    private final String instanceName;

    public HelloServiceImpl() {
        this.instanceName = "Default";
    }

    public HelloServiceImpl(String instanceName) {
        this.instanceName = instanceName;
    }

    @Override
    public String sayHello(HelloRequest request) throws RemoteException {
        System.out.println("[SERVICE " + instanceName + "] Received request from: " + request.name + " (Routing Key: " + request.routingKey + ")");
        return "Hello " + request.name + "! This is " + instanceName + " Jini service!";
    }

    public String getRoutingValue() {
        return instanceName;
    }
}
