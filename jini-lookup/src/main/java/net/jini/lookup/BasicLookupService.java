package net.jini.lookup;

import net.jini.core.discovery.LookupLocator;
import net.jini.core.lookup.*;
import net.jini.core.entry.Entry;
import net.jini.core.event.EventRegistration;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.lease.Lease;
import net.jini.core.lease.UnknownLeaseException;

import java.rmi.MarshalledObject;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A basic, in-process implementation of a Jini Lookup Service (LUS).
 */
public class BasicLookupService implements ServiceRegistrar {
    private final Map<ServiceID, ServiceItem> services = new ConcurrentHashMap<>();
    private final Map<ServiceID, BasicLease> leases = new ConcurrentHashMap<>();

    @Override
    public ServiceRegistration register(ServiceItem item, long leaseDuration) throws RemoteException {
        if (item.serviceID == null) {
            item.serviceID = new ServiceID(UUID.randomUUID().getMostSignificantBits(), UUID.randomUUID().getLeastSignificantBits());
        }
        
        // Dynamic instanceId generation for RoutingEntry
        if (item.attributeSets != null) {
            for (Entry attr : item.attributeSets) {
                if (attr instanceof net.jini.core.entry.RoutingEntry) {
                    net.jini.core.entry.RoutingEntry re = (net.jini.core.entry.RoutingEntry) attr;
                    if (re.routingKey == null || re.routingKey.isEmpty()) {
                        // Count existing instances of the same service type
                        int count = 0;
                        String serviceClass = item.service.getClass().getName();
                        for (ServiceItem existing : services.values()) {
                            if (existing.service.getClass().getName().equals(serviceClass)) {
                                count++;
                            }
                        }
                        re.routingKey = "instance-" + (count + 1);
                        System.out.println("[LUS] Assigned dynamic instanceId: " + re.routingKey + " to " + serviceClass);
                    }
                }
            }
        }
        
        services.put(item.serviceID, item);
        BasicLease lease = new BasicLease(item.serviceID, this, System.currentTimeMillis() + leaseDuration);
        leases.put(item.serviceID, lease);
        return new BasicServiceRegistration(item.serviceID, lease, this);
    }

    public void cancelLease(ServiceID serviceID) {
        services.remove(serviceID);
        leases.remove(serviceID);
        System.out.println("[LUS] Unregistered service with ID: " + serviceID);
    }

    @Override
    public Object lookup(ServiceTemplate tmpl) throws RemoteException {
        ServiceMatches matches = lookup(tmpl, 1);
        if (matches.items.length > 0) {
            return matches.items[0].service;
        }
        return null;
    }

    @Override
    public Object serviceLookup(ServiceTemplate tmpl) throws RemoteException {
        ServiceMatches matches = lookup(tmpl, 1);
        if (matches.items.length > 0) {
            Object service = matches.items[0].service;
            // For simulation, we return a RemoteServiceProxy if it's not local
            // But BasicLookupService doesn't know about containers.
            // However, if we are in LUS, we should probably return a proxy that points to the container where the service is.
            // For now, let's just return the service and see.
            return service;
        }
        return null;
    }

    @Override
    public ServiceMatches lookup(ServiceTemplate tmpl, int maxMatches) throws RemoteException {
        List<ServiceItem> matchedItems = new ArrayList<>();
        int totalMatchesCount = 0;
        for (ServiceItem item : services.values()) {
            if (matches(item, tmpl)) {
                totalMatchesCount++;
                if (matchedItems.size() < maxMatches) {
                    matchedItems.add(item);
                }
            }
        }
        return new ServiceMatches(matchedItems.toArray(new ServiceItem[0]), totalMatchesCount);
    }

    private boolean matches(ServiceItem item, ServiceTemplate tmpl) {
        if (tmpl.serviceID != null && !tmpl.serviceID.equals(item.serviceID)) {
            return false;
        }
        if (tmpl.serviceTypes != null) {
            boolean typeMatched = false;
            for (Class type : tmpl.serviceTypes) {
                if (type.isInstance(item.service)) {
                    typeMatched = true;
                    break;
                }
            }
            if (!typeMatched) return false;
        }
        if (tmpl.attributeSetTemplates != null) {
            for (Entry tmplAttr : tmpl.attributeSetTemplates) {
                if (tmplAttr == null) continue;
                boolean foundMatch = false;
                if (item.attributeSets != null) {
                    for (Entry itemAttr : item.attributeSets) {
                        if (matchesAttribute(itemAttr, tmplAttr)) {
                            foundMatch = true;
                            break;
                        }
                    }
                }
                if (!foundMatch) return false;
            }
        }
        return true;
    }

    private boolean matchesAttribute(Entry itemAttr, Entry tmplAttr) {
        if (itemAttr == null || tmplAttr == null) return false;
        if (!tmplAttr.getClass().isInstance(itemAttr)) return false;
        
        // Use reflection to match non-null fields in tmplAttr
        for (java.lang.reflect.Field field : tmplAttr.getClass().getFields()) {
            try {
                Object tmplVal = field.get(tmplAttr);
                if (tmplVal != null) {
                    Object itemVal = field.get(itemAttr);
                    if (!tmplVal.equals(itemVal)) {
                        return false;
                    }
                }
            } catch (IllegalAccessException e) {
                // Ignore fields we can't access
            }
        }
        return true;
    }

    @Override
    public EventRegistration notify(ServiceTemplate tmpl, int transitions, RemoteEventListener listener, MarshalledObject handback, long leaseDuration) throws RemoteException {
        // Basic implementation would manage a list of listeners
        return null;
    }

    @Override
    public Class[] getEntryClasses(ServiceTemplate tmpl) throws RemoteException {
        Set<Class> classes = new HashSet<>();
        for (ServiceItem item : services.values()) {
            if (matches(item, tmpl) && item.attributeSets != null) {
                for (Entry entry : item.attributeSets) {
                    if (entry != null) classes.add(entry.getClass());
                }
            }
        }
        return classes.toArray(new Class[0]);
    }

    @Override
    public Object[] getFieldValues(ServiceTemplate tmpl, int setIndex, String fieldName) throws NoSuchFieldException, RemoteException {
        List<Object> values = new ArrayList<>();
        for (ServiceItem item : services.values()) {
            if (matches(item, tmpl) && item.attributeSets != null && setIndex < item.attributeSets.length) {
                Entry entry = item.attributeSets[setIndex];
                if (entry != null) {
                    try {
                        java.lang.reflect.Field field = entry.getClass().getField(fieldName);
                        values.add(field.get(entry));
                    } catch (IllegalAccessException e) {
                        // skip
                    }
                }
            }
        }
        return values.toArray();
    }

    @Override
    public Class[] getServiceTypes(ServiceTemplate tmpl, String prefix) throws RemoteException {
        Set<Class> types = new HashSet<>();
        for (ServiceItem item : services.values()) {
            if (matches(item, tmpl)) {
                Class cls = item.service.getClass();
                while (cls != null) {
                    if (cls.getName().startsWith(prefix)) types.add(cls);
                    for (Class intf : cls.getInterfaces()) {
                        if (intf.getName().startsWith(prefix)) types.add(intf);
                    }
                    cls = cls.getSuperclass();
                }
            }
        }
        return types.toArray(new Class[0]);
    }

    @Override
    public ServiceID getServiceID() {
        return new ServiceID(0, 0); // Should be unique per LUS
    }

    @Override
    public LookupLocator getLocator() throws RemoteException {
        String host = System.getProperty("lus.host", "localhost");
        int port = Integer.getInteger("lus.port", 10999);
        return new LookupLocator(host, port);
    }

    @Override
    public String[] getGroups() throws RemoteException {
        return new String[] { "" }; // Default group
    }

    private static class BasicServiceRegistration implements ServiceRegistration, java.io.Serializable {
        private static final long serialVersionUID = 1L;
        private final ServiceID serviceID;
        private final Lease lease;
        private final transient BasicLookupService lus;

        public BasicServiceRegistration(ServiceID serviceID, Lease lease, BasicLookupService lus) {
            this.serviceID = serviceID;
            this.lease = lease;
            this.lus = lus;
        }

        @Override public ServiceID getServiceID() { return serviceID; }
        @Override public Lease getLease() { return lease; }
        @Override public void setAttributes(Entry[] attrSets) throws UnknownLeaseException, RemoteException {}
        @Override public void addAttributes(Entry[] attrSets) throws UnknownLeaseException, RemoteException {}
        @Override public void modifyAttributes(Entry[] attrSetTemplates, Entry[] attrSets) throws UnknownLeaseException, RemoteException {}
    }

    private static class BasicLease implements Lease, java.io.Serializable {
        private static final long serialVersionUID = 1L;
        private final ServiceID serviceID;
        private final transient BasicLookupService lus;
        private long expiration;

        public BasicLease(ServiceID serviceID, BasicLookupService lus, long expiration) {
            this.serviceID = serviceID;
            this.lus = lus;
            this.expiration = expiration;
        }

        @Override public long getExpiration() { return expiration; }
        @Override public void renew(long duration) { this.expiration = System.currentTimeMillis() + duration; }
        @Override
        public void cancel() {
            if (lus != null) {
                lus.cancelLease(serviceID);
            }
        }
    }
}
