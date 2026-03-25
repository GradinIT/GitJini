package net.jini.discovery;

import net.jini.core.lookup.ServiceRegistrar;
import java.util.EventObject;
import java.util.Map;

/**
 * Event that contains discovered lookup services (registrars).
 */
public class DiscoveryEvent extends EventObject {
    private final ServiceRegistrar[] registrars;
    private final Map<ServiceRegistrar, String[]> groups;

    public DiscoveryEvent(Object source, ServiceRegistrar[] registrars) {
        this(source, registrars, null);
    }

    public DiscoveryEvent(Object source, ServiceRegistrar[] registrars, Map<ServiceRegistrar, String[]> groups) {
        super(source);
        this.registrars = registrars;
        this.groups = groups;
    }

    public ServiceRegistrar[] getRegistrars() {
        return registrars;
    }

    public Map<ServiceRegistrar, String[]> getGroups() {
        return groups;
    }
}
