package net.jini.grid;

import net.jini.core.export.ExportedService;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.discovery.DiscoveryService;
import net.jini.export.ServiceExporter;
import net.jini.lookup.BasicLookupService;
import java.io.Serializable;
import java.rmi.RemoteException;

@ExportedService(id = "DSA")
public class DistributedServiceAgentImpl implements DistributedServiceAgent, Serializable {
    private static final long serialVersionUID = 1L;

    @Override
    public void startDSM() throws RemoteException {
        System.out.println("[DSA] Starting DSM component...");
        try {
            ServiceExporter.exportIfNeeded(new DistributedServiceManagerImpl());
        } catch (Exception e) {
            throw new RemoteException("Failed to start DSM", e);
        }
    }

    @Override
    public void startDSC() throws RemoteException {
        System.out.println("[DSA] Starting DSC component...");
        try {
            ServiceExporter.exportIfNeeded(new DistributedServiceContainerImpl());
        } catch (Exception e) {
            throw new RemoteException("Failed to start DSC", e);
        }
    }

    @Override
    public void startLUS() throws RemoteException {
        System.out.println("[DSA] Starting LUS component...");
        try {
            ServiceRegistrar registrar = new BasicLookupService();
            String lusHost = System.getProperty("lus.host", "0.0.0.0");
            int lusPort = Integer.getInteger("lus.port", 10999);
            DiscoveryService.register(lusHost, lusPort, registrar);
        } catch (Exception e) {
            throw new RemoteException("Failed to start LUS", e);
        }
    }

    @Override
    public boolean isAlive() throws RemoteException {
        return true;
    }
}
