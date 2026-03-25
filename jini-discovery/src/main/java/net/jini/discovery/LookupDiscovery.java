package net.jini.discovery;

import net.jini.core.lookup.ServiceRegistrar;
import java.io.IOException;
import java.net.*;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Basic implementation of the Multicast Request Protocol.
 */
public class LookupDiscovery {
    private final String[] groups;
    private final Set<ServiceRegistrar> registrars = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<DiscoveryListener> listeners = new CopyOnWriteArraySet<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public LookupDiscovery(String[] groups) {
        this.groups = groups;
        // Mocking discovery for now - in a real implementation this would use multicast
        startDiscovery();
    }

    private void startDiscovery() {
        executor.submit(() -> {
            // Simulated delay for discovery
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            // Implementation detail: check DiscoveryService in this simulation
        });
    }

    public void addDiscoveryListener(DiscoveryListener l) {
        listeners.add(l);
        // If we already have registrars, notify the new listener
        if (!registrars.isEmpty()) {
            DiscoveryEvent ev = new DiscoveryEvent(this, getRegistrars());
            l.discovered(ev);
        }
    }

    public void removeDiscoveryListener(DiscoveryListener l) {
        listeners.remove(l);
    }

    public void terminate() {
        executor.shutdownNow();
    }

    public ServiceRegistrar[] getRegistrars() {
        return registrars.toArray(new ServiceRegistrar[0]);
    }

    /**
     * For simulation purposes, manually add a registrar.
     */
    public void addRegistrar(ServiceRegistrar registrar) {
        if (registrars.add(registrar)) {
            DiscoveryEvent ev = new DiscoveryEvent(this, new ServiceRegistrar[]{registrar});
            for (DiscoveryListener l : listeners) {
                l.discovered(ev);
            }
        }
    }
}
