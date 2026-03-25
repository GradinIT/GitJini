package net.jini.grid;

import net.jini.core.lookup.ServiceID;
import java.io.Serializable;
import java.util.List;

public class ServiceUnit implements Serializable {
    private final String name;
    private final List<Object> services;
    private final SLA sla;
    private final boolean embeddedSpace;

    public ServiceUnit(String name, List<Object> services) {
        this(name, services, new SLA(), false);
    }

    public ServiceUnit(String name, List<Object> services, SLA sla) {
        this(name, services, sla, false);
    }

    public ServiceUnit(String name, List<Object> services, SLA sla, boolean embeddedSpace) {
        this.name = name;
        this.services = services;
        this.sla = sla;
        this.embeddedSpace = embeddedSpace;
    }

    public String getName() {
        return name;
    }

    public List<Object> getServices() {
        return services;
    }

    public SLA getSla() {
        return sla;
    }

    public boolean hasEmbeddedSpace() {
        return embeddedSpace;
    }
}
