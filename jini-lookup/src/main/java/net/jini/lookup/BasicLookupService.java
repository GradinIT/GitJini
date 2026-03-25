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

    @Override
    public ServiceRegistration register(ServiceItem item, long leaseDuration) throws RemoteException {
        if (item.serviceID == null) {
            item.serviceID = new ServiceID(UUID.randomUUID().getMostSignificantBits(), UUID.randomUUID().getLeastSignificantBits());
        }
        services.put(item.serviceID, item);
        return new BasicServiceRegistration(item.serviceID, this);
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
    public ServiceMatches lookup(ServiceTemplate tmpl, int maxMatches) throws RemoteException {
        List<ServiceItem> matchedItems = new ArrayList<>();
        for (ServiceItem item : services.values()) {
            if (matches(item, tmpl)) {
                matchedItems.add(item);
            }
            if (matchedItems.size() >= maxMatches) break;
        }
        return new ServiceMatches(matchedItems.toArray(new ServiceItem[0]), services.size());
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
        int port = Integer.getInteger("lus.port", 1099);
        return new LookupLocator(host, port);
    }

    @Override
    public String[] getGroups() throws RemoteException {
        return new String[] { "" }; // Default group
    }

    private static class BasicServiceRegistration implements ServiceRegistration {
        private final ServiceID serviceID;
        private final BasicLookupService lus;

        public BasicServiceRegistration(ServiceID serviceID, BasicLookupService lus) {
            this.serviceID = serviceID;
            this.lus = lus;
        }

        @Override public ServiceID getServiceID() { return serviceID; }
        @Override public Lease getLease() { return null; }
        @Override public void setAttributes(Entry[] attrSets) throws UnknownLeaseException, RemoteException {}
        @Override public void addAttributes(Entry[] attrSets) throws UnknownLeaseException, RemoteException {}
        @Override public void modifyAttributes(Entry[] attrSetTemplates, Entry[] attrSets) throws UnknownLeaseException, RemoteException {}
    }
}
