package net.jini.export;

import net.jini.core.export.ExportedService;
import net.jini.core.export.ServiceRouting;
import net.jini.core.lookup.*;
import net.jini.core.entry.Entry;
import net.jini.core.entry.RoutingEntry;
import net.jini.core.discovery.LookupLocator;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Utility to automatically register services annotated with @ExportedService.
 */
public class ServiceExporter {

    /**
     * Checks if the given object is annotated with @ExportedService and, if so, 
     * registers it with the Lookup Service specified by lus.host and lus.port.
     */
    public static ServiceRegistration exportIfNeeded(Object service) throws Exception {
        Class<?> serviceClass = service.getClass();
        ExportedService annotation = serviceClass.getAnnotation(ExportedService.class);
        
        // If not annotated, we still export it if it's a known service type (like JavaSpace)
        boolean isSpace = false;
        try {
            Class<?> spaceInterface = Class.forName("net.jini.space.JavaSpace");
            if (spaceInterface.isInstance(service)) {
                isSpace = true;
            }
        } catch (ClassNotFoundException ignored) {}

        if (annotation == null && !isSpace) {
            return null;
        }
        
        String host = System.getProperty("lus.host", "localhost");
        int port = Integer.getInteger("lus.port", 10999);
        
        System.out.println("[EXPORTER] Exporting service " + serviceClass.getName() + " to " + host + ":" + port);
        
        LookupLocator locator = new LookupLocator(host, port);
        ServiceRegistrar registrar = null;
        
        // Retry logic for initial connection to LUS
        for (int i = 0; i < 20; i++) {
            try {
                registrar = locator.getRegistrar();
                if (registrar != null) break;
                System.out.println("[EXPORTER] LUS not yet ready at " + host + ":" + port + "... (Attempt " + (i+1) + "/20)");
            } catch (Exception e) {
                System.out.println("[EXPORTER] Waiting for Lookup Service at " + host + ":" + port + "... (Attempt " + (i+1) + "/20). Error: " + e.getMessage());
            }
            Thread.sleep(5000);
        }
        
        if (registrar == null) {
            throw new IllegalStateException("Could not find Lookup Service at " + host + ":" + port + " after 20 attempts");
        }
        
        ServiceID serviceID;
        String instanceId = annotation != null ? annotation.instanceId() : "";
        String id = annotation != null ? annotation.id() : "";

        if (id.isEmpty()) {
            serviceID = new ServiceID(UUID.randomUUID().getMostSignificantBits(), UUID.randomUUID().getLeastSignificantBits());
        } else {
            String compositeId = id;
            if (!instanceId.isEmpty()) {
                compositeId += ":" + instanceId;
                UUID uuid = UUID.nameUUIDFromBytes(compositeId.getBytes());
                serviceID = new ServiceID(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
            } else {
                // Let LUS generate ServiceID or use random to avoid collisions
                serviceID = new ServiceID(UUID.randomUUID().getMostSignificantBits(), UUID.randomUUID().getLeastSignificantBits());
            }
        }
        
        List<Entry> attributes = new ArrayList<>();
        // Always add a RoutingEntry, use the instanceId from @ExportedService if present
        attributes.add(new RoutingEntry(instanceId.isEmpty() ? null : instanceId));
        
        ServiceItem item = new ServiceItem(serviceID, service, attributes.toArray(new Entry[0]));
        
        // Register for 5 minutes by default
        ServiceRegistration reg = null;
        for (int i = 0; i < 5; i++) {
            try {
                reg = registrar.register(item, 1000 * 60 * 5);
                if (reg != null) break;
                System.out.println("[EXPORTER] Registrar returned null for " + serviceClass.getName() + ", retrying... (Attempt " + (i+1) + "/5)");
            } catch (Exception e) {
                System.err.println("[EXPORTER] Registration failed for " + serviceClass.getName() + ": " + e.getMessage() + ", retrying... (Attempt " + (i+1) + "/5)");
                if (e.getCause() instanceof java.io.NotSerializableException || e instanceof java.io.NotSerializableException) {
                    e.printStackTrace();
                }
            }
            Thread.sleep(2000);
        }

        if (reg == null) {
            throw new IllegalStateException("Could not register service " + serviceClass.getName() + " with the Lookup Service");
        }

        System.out.println("[EXPORTER] Service registered with ID: " + reg.getServiceID());
        return reg;
    }
}
