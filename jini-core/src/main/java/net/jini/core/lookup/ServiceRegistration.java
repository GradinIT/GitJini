package net.jini.core.lookup;

import net.jini.core.lease.Lease;
import net.jini.core.lease.UnknownLeaseException;
import net.jini.core.entry.Entry;
import java.rmi.RemoteException;

/**
 * ServiceRegistration is returned after a service registers with a LUS.
 */
public interface ServiceRegistration {
    ServiceID getServiceID();
    Lease getLease();
    void setAttributes(Entry[] attrSets) throws UnknownLeaseException, RemoteException;
    void addAttributes(Entry[] attrSets) throws UnknownLeaseException, RemoteException;
    void modifyAttributes(Entry[] attrSetTemplates, Entry[] attrSets) throws UnknownLeaseException, RemoteException;
}
