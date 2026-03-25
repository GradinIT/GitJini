package net.jini.core.lookup;

import net.jini.core.entry.Entry;
import java.io.Serializable;

/**
 * A ServiceItem contains the ServiceID, the service object (proxy), and its 
 * attributes (Entries).
 */
public class ServiceItem implements Serializable {
    private static final long serialVersionUID = 1L;

    public ServiceID serviceID;
    public Object service;
    public Entry[] attributeSets;

    public ServiceItem(ServiceID serviceID, Object service, Entry[] attributeSets) {
        this.serviceID = serviceID;
        this.service = service;
        this.attributeSets = attributeSets;
    }
}
