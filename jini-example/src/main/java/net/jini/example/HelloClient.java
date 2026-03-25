package net.jini.example;

import net.jini.core.export.ImportService;
import java.rmi.RemoteException;

public class HelloClient {

    @ImportService
    private HelloService service;

    public String callHello(String routingKey, String name) throws RemoteException {
        if (service == null) {
            throw new IllegalStateException("HelloService not injected!");
        }
        System.out.println("[HELLO CLIENT] Calling service via injected proxy...");
        return service.sayHello(new HelloRequest(routingKey, name));
    }
    
    public HelloService getService() {
        return service;
    }
}
