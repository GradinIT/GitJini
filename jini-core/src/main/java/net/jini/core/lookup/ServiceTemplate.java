package net.jini.core.lookup;

import net.jini.core.entry.Entry;
import java.io.Serializable;

/**
 * ServiceTemplate is used for matching services during lookup.
 */
public class ServiceTemplate implements Serializable {
    private static final long serialVersionUID = 1L;

    public ServiceID serviceID;
    public Class[] serviceTypes;
    public Entry[] attributeSetTemplates;

    public ServiceTemplate(ServiceID serviceID, Class[] serviceTypes, Entry[] attrSetTemplates) {
        this.serviceID = serviceID;
        this.serviceTypes = serviceTypes;
        this.attributeSetTemplates = attrSetTemplates;
    }
}
