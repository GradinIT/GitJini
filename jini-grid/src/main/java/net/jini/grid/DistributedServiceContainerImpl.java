package net.jini.grid;

import net.jini.core.export.ExportedService;
import net.jini.core.lookup.ServiceRegistration;
import net.jini.export.ServiceExporter;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ExportedService(id = "DSC")
public class DistributedServiceContainerImpl implements DistributedServiceContainer {
    private final Map<String, ServiceUnit> deployedUnits = new HashMap<>();
    private final Map<String, List<ServiceRegistration>> unitRegistrations = new HashMap<>();

    @Override
    public void deploy(ServiceUnit unit) throws RemoteException {
        System.out.println("[DSC] Deploying ServiceUnit: " + unit.getName());
        deployedUnits.put(unit.getName(), unit);
        List<ServiceRegistration> registrations = new ArrayList<>();

        // Handle embedded space if present in SU
        if (unit.hasEmbeddedSpace()) {
            System.out.println("[DSC] Starting embedded space for unit: " + unit.getName());
            try {
                // Avoid compile-time dependency on jini-java-spaces; instantiate via reflection if available
                Class<?> spaceClass = Class.forName("net.jini.space.BasicJavaSpace");
                Object space = spaceClass.getDeclaredConstructor().newInstance();
                ServiceRegistration reg = ServiceExporter.exportIfNeeded(space);
                if (reg != null) {
                    registrations.add(reg);
                }
            } catch (ClassNotFoundException cnfe) {
                System.err.println("[DSC] BasicJavaSpace not found on classpath; embedded space will not be started.");
            } catch (Exception e) {
                throw new RemoteException("Failed to export embedded space in ServiceUnit: " + unit.getName(), e);
            }
        }

        for (Object service : unit.getServices()) {
            try {
                ServiceRegistration reg = ServiceExporter.exportIfNeeded(service);
                if (reg != null) {
                    registrations.add(reg);
                }
            } catch (Exception e) {
                throw new RemoteException("Failed to export service in ServiceUnit: " + unit.getName(), e);
            }
        }
        unitRegistrations.put(unit.getName(), registrations);
    }

    @Override
    public void undeploy(String unitName) throws RemoteException {
        System.out.println("[DSC] Undeploying ServiceUnit: " + unitName);
        deployedUnits.remove(unitName);
        List<ServiceRegistration> registrations = unitRegistrations.remove(unitName);
        if (registrations != null) {
            for (ServiceRegistration reg : registrations) {
                try {
                    reg.getLease().cancel();
                    System.out.println("[DSC] Cancelled lease for service ID: " + reg.getServiceID());
                } catch (Exception e) {
                    System.err.println("[DSC] Failed to cancel lease for service ID: " + reg.getServiceID() + " : " + e.getMessage());
                }
            }
        }
    }

    @Override
    public List<String> getDeployedUnits() throws RemoteException {
        return new ArrayList<>(deployedUnits.keySet());
    }

    @Override
    public boolean isAlive() throws RemoteException {
        return true;
    }
}
