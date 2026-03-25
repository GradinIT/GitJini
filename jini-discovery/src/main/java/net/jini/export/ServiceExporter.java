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
        if (serviceClass.isAnnotationPresent(ExportedService.class)) {
            ExportedService annotation = serviceClass.getAnnotation(ExportedService.class);
            
            String host = System.getProperty("lus.host", "localhost");
            int port = Integer.getInteger("lus.port", 1099);
            
            System.out.println("[EXPORTER] Exporting service " + serviceClass.getName());
            
            LookupLocator locator = new LookupLocator(host, port);
            ServiceRegistrar registrar = locator.getRegistrar();
            
            if (registrar == null) {
                throw new IllegalStateException("Could not find Lookup Service at " + host + ":" + port);
            }
            
            ServiceID serviceID;
            String instanceId = annotation.instanceId();
            if (annotation.id().isEmpty()) {
                serviceID = new ServiceID(UUID.randomUUID().getMostSignificantBits(), UUID.randomUUID().getLeastSignificantBits());
            } else {
                String compositeId = annotation.id();
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
            ServiceRegistration reg = registrar.register(item, 1000 * 60 * 5);
            System.out.println("[EXPORTER] Service registered with ID: " + reg.getServiceID());
            return reg;
        }
        return null;
    }
}
