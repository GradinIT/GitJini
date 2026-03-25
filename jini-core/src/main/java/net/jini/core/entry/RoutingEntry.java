package net.jini.core.entry;

import java.io.Serializable;

/**
 * A specialized Entry to hold a routing value for service instances.
 */
public class RoutingEntry implements Entry, Serializable {
    private static final long serialVersionUID = 1L;

    public String routingKey;

    public RoutingEntry() {
    }

    public RoutingEntry(String routingKey) {
        this.routingKey = routingKey;
    }

    @Override
    public String toString() {
        return "RoutingEntry{routingKey='" + routingKey + "'}";
    }
}
