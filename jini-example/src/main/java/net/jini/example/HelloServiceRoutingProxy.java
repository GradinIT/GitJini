package net.jini.example;

import net.jini.core.lookup.*;
import net.jini.core.entry.RoutingEntry;
import net.jini.core.export.ServiceRouting;
import net.jini.core.entry.Entry;
import java.lang.reflect.Field;
import java.rmi.RemoteException;

/**
 * A "Smart Proxy" or client-side router that uses the ServiceRegistrar
 * to route calls based on the @ServiceRouting annotation in the payload.
 */
public class HelloServiceRoutingProxy implements HelloService {
    private final ServiceRegistrar registrar;

    public HelloServiceRoutingProxy(ServiceRegistrar registrar) {
        this.registrar = registrar;
    }

    @Override
    public String sayHello(HelloRequest request) throws RemoteException {
        String routingKey = extractRoutingKey(request);
        System.out.println("[ROUTING PROXY] Extracted routing key: " + routingKey);
        
        // Find the service that matches this routing key
        Entry[] routingAttrs = new Entry[] { new RoutingEntry(routingKey) };
        ServiceTemplate template = new ServiceTemplate(null, new Class[]{HelloService.class}, routingAttrs);
        
        Object service = registrar.lookup(template);
        if (service instanceof HelloService) {
            System.out.println("[ROUTING PROXY] Routing call to instance with routing key: " + routingKey);
            return ((HelloService) service).sayHello(request);
        } else {
            throw new RemoteException("No service instance found for routing key: " + routingKey);
        }
    }

    private String extractRoutingKey(Object payload) {
        for (Field field : payload.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(ServiceRouting.class)) {
                try {
                    field.setAccessible(true);
                    Object val = field.get(payload);
                    return val != null ? val.toString() : null;
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Could not access @ServiceRouting field", e);
                }
            }
        }
        return null;
    }
}
