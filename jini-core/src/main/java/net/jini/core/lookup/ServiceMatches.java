package net.jini.core.lookup;

import java.io.Serializable;

/**
 * ServiceMatches contains the results of a multi-item lookup.
 */
public class ServiceMatches implements Serializable {
    private static final long serialVersionUID = 1L;

    public ServiceItem[] items;
    public int totalMatches;

    public ServiceMatches(ServiceItem[] items, int totalMatches) {
        this.items = items;
        this.totalMatches = totalMatches;
    }
}
