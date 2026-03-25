package net.jini.core.lookup;

import net.jini.core.lease.Lease;
import net.jini.core.entry.Entry;
import net.jini.core.event.EventRegistration;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.discovery.LookupLocator;

import java.io.Serializable;
import java.rmi.MarshalledObject;
import java.rmi.RemoteException;

/**
 * ServiceRegistrar represents the lookup service's proxy.
 */
public interface ServiceRegistrar extends Serializable {
    int TRANSITION_MATCH_NOMATCH = 1 << 0;
    int TRANSITION_NOMATCH_MATCH = 1 << 1;
    int TRANSITION_MATCH_MATCH = 1 << 2;

    ServiceRegistration register(ServiceItem item, long leaseDuration) throws RemoteException;
    Object lookup(ServiceTemplate tmpl) throws RemoteException;
    ServiceMatches lookup(ServiceTemplate tmpl, int maxMatches) throws RemoteException;
    Object serviceLookup(ServiceTemplate tmpl) throws RemoteException;

    EventRegistration notify(ServiceTemplate tmpl,
                             int transitions,
                             RemoteEventListener listener,
                             MarshalledObject handback,
                             long leaseDuration) throws RemoteException;

    Class[] getEntryClasses(ServiceTemplate tmpl) throws RemoteException;
    Object[] getFieldValues(ServiceTemplate tmpl, int setIndex, String field) 
        throws NoSuchFieldException, RemoteException;
    Class[] getServiceTypes(ServiceTemplate tmpl, String prefix) throws RemoteException;

    ServiceID getServiceID();
    LookupLocator getLocator() throws RemoteException;
    String[] getGroups() throws RemoteException;
}
