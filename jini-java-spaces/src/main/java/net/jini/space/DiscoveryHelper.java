package net.jini.space;

import net.jini.core.lookup.ServiceID;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.lookup.ServiceRegistration;
import net.jini.core.entry.Entry;
import net.jini.discovery.DiscoveryEvent;
import net.jini.discovery.DiscoveryListener;
import net.jini.discovery.LookupDiscovery;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A helper class to facilitate registration of a JavaSpace with a Jini Lookup Service.
 */
public class DiscoveryHelper {

    /**
     * Registers a JavaSpace with the given registrar.
     *
     * @param space the JavaSpace implementation to register
     * @param registrar the lookup service registrar
     * @param attributes the attributes (Entry[]) to associate with the service
     * @param leaseDuration the requested lease duration
     * @return the ServiceRegistration returned by the registrar
     * @throws RemoteException if a communication error occurs
     */
    public static ServiceRegistration register(JavaSpace space,
                                             ServiceRegistrar registrar,
                                             Entry[] attributes,
                                             long leaseDuration) throws RemoteException {

        UUID uuid = UUID.randomUUID();
        ServiceID serviceID = new ServiceID(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
        ServiceItem item = new ServiceItem(serviceID, space, attributes);

        return registrar.register(item, leaseDuration);
    }

    /**
     * Automatically discovers registrars and registers the JavaSpace with all of them.
     * This method is synchronous and only registers with registrars already discovered at call time.
     *
     * @param space the JavaSpace implementation to register
     * @param groups the discovery groups to search for
     * @param attributes the attributes (Entry[]) to associate with the service
     * @param leaseDuration the requested lease duration
     * @return a list of ServiceRegistrations
     * @throws RemoteException if a communication error occurs
     * @deprecated Use {@link #beginAutoRegistration(JavaSpace, String[], Entry[], long)} for truly automatic registration.
     */
    @Deprecated
    public static List<ServiceRegistration> autoRegister(JavaSpace space,
                                                       String[] groups,
                                                       Entry[] attributes,
                                                       long leaseDuration) throws RemoteException {
        LookupDiscovery discovery = new LookupDiscovery(groups);
        ServiceRegistrar[] registrars = discovery.getRegistrars();
        List<ServiceRegistration> registrations = new ArrayList<>();

        for (ServiceRegistrar registrar : registrars) {
            registrations.add(register(space, registrar, attributes, leaseDuration));
        }

        return registrations;
    }

    /**
     * Begins automatic registration of a JavaSpace with all discovered registrars,
     * including those discovered in the future.
     *
     * @param space the JavaSpace implementation to register
     * @param groups the discovery groups to search for
     * @param attributes the attributes (Entry[]) to associate with the service
     * @param leaseDuration the requested lease duration
     * @return an AutoRegistration object to manage the process
     */
    public static AutoRegistration beginAutoRegistration(JavaSpace space,
                                                         String[] groups,
                                                         Entry[] attributes,
                                                         long leaseDuration) {
        LookupDiscovery discovery = new LookupDiscovery(groups);
        AutoRegistration autoReg = new AutoRegistration(space, discovery, attributes, leaseDuration);
        discovery.addDiscoveryListener(autoReg);
        return autoReg;
    }

    /**
     * Manages automatic registration of a JavaSpace.
     */
    public static class AutoRegistration implements DiscoveryListener {
        private final JavaSpace space;
        private final LookupDiscovery discovery;
        private final Entry[] attributes;
        private final long leaseDuration;
        private final List<ServiceRegistration> registrations = new CopyOnWriteArrayList<>();

        private AutoRegistration(JavaSpace space, LookupDiscovery discovery, Entry[] attributes, long leaseDuration) {
            this.space = space;
            this.discovery = discovery;
            this.attributes = attributes;
            this.leaseDuration = leaseDuration;
        }

        @Override
        public void discovered(DiscoveryEvent event) {
            for (ServiceRegistrar registrar : event.getRegistrars()) {
                try {
                    registrations.add(register(space, registrar, attributes, leaseDuration));
                } catch (RemoteException e) {
                    System.err.println("[DiscoveryHelper] Failed to register with registrar: " + e.getMessage());
                }
            }
        }

        @Override
        public void discarded(DiscoveryEvent event) {
            // In a more complete implementation, we might remove matching registrations
        }

        public void terminate() {
            discovery.removeDiscoveryListener(this);
            discovery.terminate();
        }

        public List<ServiceRegistration> getRegistrations() {
            return new ArrayList<>(registrations);
        }
    }
}
