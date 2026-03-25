package net.jini.export;

import net.jini.core.export.ImportService;
import net.jini.core.export.ServiceRouting;
import net.jini.core.lookup.*;
import net.jini.core.entry.Entry;
import net.jini.core.entry.RoutingEntry;
import net.jini.core.discovery.LookupLocator;

import java.lang.reflect.*;
import java.rmi.RemoteException;

/**
 * Utility to automatically inject routing proxies into fields annotated with @ImportService.
 */
public class ServiceImporter {

    public static void importServices(Object target, ServiceRegistrar registrar) throws Exception {
        for (Field field : target.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(ImportService.class)) {
                Class<?> serviceType = field.getType();
                Object proxy = createRoutingProxy(serviceType, registrar);
                field.setAccessible(true);
                field.set(target, proxy);
                System.out.println("[IMPORTER] Injected routing proxy for " + serviceType.getSimpleName() + " into " + target.getClass().getSimpleName());
            }
        }
    }

    private static Object createRoutingProxy(final Class<?> serviceType, final ServiceRegistrar registrar) {
        return Proxy.newProxyInstance(
            serviceType.getClassLoader(),
            new Class<?>[]{serviceType},
            new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    if (args != null && args.length > 0) {
                        Object payload = args[0];
                        String routingKey = extractRoutingKey(payload);
                        if (routingKey != null) {
                            // Calculate actual instance routing key if needed (e.g. for load balancing)
                            String targetRoutingKey = calculateTargetRoutingKey(routingKey, registrar, serviceType);
                            
                            System.out.println("[GENERIC ROUTING PROXY] Routing key: " + routingKey + " -> Target: " + targetRoutingKey);
                            Entry[] routingAttrs = new Entry[]{new RoutingEntry(targetRoutingKey)};
                            ServiceTemplate template = new ServiceTemplate(null, new Class[]{serviceType}, routingAttrs);
                            Object actualService = registrar.lookup(template);
                            if (actualService != null) {
                                try {
                                    return method.invoke(actualService, args);
                                } catch (InvocationTargetException e) {
                                    throw e.getCause();
                                }
                            } else {
                                throw new RemoteException("No service instance found for routing key: " + targetRoutingKey + " (original: " + routingKey + ")");
                            }
                        }
                    }
                    
                    // Fallback to normal lookup if no routing key or no args
                    ServiceTemplate template = new ServiceTemplate(null, new Class[]{serviceType}, null);
                    Object actualService = registrar.lookup(template);
                    if (actualService != null) {
                        try {
                            return method.invoke(actualService, args);
                        } catch (InvocationTargetException e) {
                            throw e.getCause();
                        }
                    }
                    throw new RemoteException("No service instance found for " + serviceType.getName());
                }
            }
        );
    }

    private static String calculateTargetRoutingKey(String routingKey, ServiceRegistrar registrar, Class<?> serviceType) throws RemoteException {
        // If the routing key directly matches an instance, use it
        ServiceTemplate exactTmpl = new ServiceTemplate(null, new Class[]{serviceType}, new Entry[]{new RoutingEntry(routingKey)});
        if (registrar.lookup(exactTmpl) != null) {
            return routingKey;
        }

        // Otherwise, discover all available instances and pick one based on hash
        ServiceTemplate allTmpl = new ServiceTemplate(null, new Class[]{serviceType}, new Entry[]{new RoutingEntry(null)});
        ServiceMatches matches = registrar.lookup(allTmpl, 100);
        
        if (matches.items.length == 0) {
            return routingKey; // Fallback to original
        }

        // Collect all routing keys
        java.util.List<String> availableKeys = new java.util.ArrayList<>();
        for (ServiceItem item : matches.items) {
            if (item.attributeSets != null) {
                for (Entry attr : item.attributeSets) {
                    if (attr instanceof RoutingEntry) {
                        String key = ((RoutingEntry) attr).routingKey;
                        if (key != null && !availableKeys.contains(key)) {
                            availableKeys.add(key);
                        }
                    }
                }
            }
        }

        if (availableKeys.isEmpty()) {
            return routingKey;
        }

        // Sort for deterministic selection
        java.util.Collections.sort(availableKeys);
        
        // Use hash of routingKey to select an index
        int hash = Math.abs(routingKey.hashCode());
        int index = hash % availableKeys.size();
        
        return availableKeys.get(index);
    }

    private static String extractRoutingKey(Object payload) {
        if (payload == null) return null;
        for (Field field : payload.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(ServiceRouting.class)) {
                try {
                    field.setAccessible(true);
                    Object val = field.get(payload);
                    return val != null ? val.toString() : null;
                } catch (IllegalAccessException e) {
                    // Ignore
                }
            }
        }
        return null;
    }
}
