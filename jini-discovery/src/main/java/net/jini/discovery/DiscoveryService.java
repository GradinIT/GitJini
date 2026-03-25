package net.jini.discovery;

import net.jini.core.lookup.ServiceRegistrar;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A registry for simulated discovery services. 
 * Allows lookup services to register themselves for local discovery.
 */
public class DiscoveryService {
    private static final Map<String, ServiceRegistrar> registry = new ConcurrentHashMap<>();

    public static void register(String host, int port, ServiceRegistrar registrar) {
        registry.put(host + ":" + port, registrar);
    }

    public static ServiceRegistrar getRegistrar(String host, int port) {
        return registry.get(host + ":" + port);
    }
}
