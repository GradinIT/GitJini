package net.jini.grid;

import net.jini.core.discovery.LookupLocator;
import net.jini.core.export.ExportedService;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceMatches;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.lookup.ServiceTemplate;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

@ExportedService(id = "DSM")
public class DistributedServiceManagerImpl implements DistributedServiceManager {

    @Override
    public void deploy(ServiceUnit unit) throws RemoteException {
        System.out.println("[DSM] Deploying ServiceUnit: " + unit.getName() + " with SLA: " + unit.getSla());
        
        SLA sla = unit.getSla();
        int instances = sla.getNumberOfInstances();
        int backups = sla.getNumberOfBackups();
        
        if ("partitioned".equals(sla.getClusterSchema())) {
            for (int i = 0; i < instances; i++) {
                deployInstance(unit, i, 0); // Primary
                for (int b = 1; b <= backups; b++) {
                    deployInstance(unit, i, b); // Backups
                }
            }
        } else if ("sync-replicated".equals(sla.getClusterSchema()) || "async-replicated".equals(sla.getClusterSchema())) {
            for (int i = 0; i < instances; i++) {
                deployInstance(unit, i, 0);
            }
        } else {
            // default
            deployInstance(unit, 0, 0);
        }
    }

    private void deployInstance(ServiceUnit unit, int instanceId, int backupId) throws RemoteException {
        String name = unit.getName() + (instanceId > 0 || backupId > 0 ? " (" + instanceId + (backupId > 0 ? ":" + backupId : "") + ")" : "");
        System.out.println("[DSM] Deploying instance " + instanceId + (backupId > 0 ? " backup " + backupId : "") + " of " + unit.getName());
        
        DistributedServiceContainer dsc = findAvailableDSC();
        if (dsc == null) {
            throw new RemoteException("No available DSC found to deploy " + name);
        }
        
        // Add a small delay between concurrent deployments to avoid Discovery Server congestion in simulation
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Pass instance/backup info via Space URL if it's an embedded space
        if (unit.hasEmbeddedSpace()) {
            SLA sla = unit.getSla();
            String totalMembers = sla.getNumberOfInstances() + "," + sla.getNumberOfBackups();
            String baseUrl = unit.getSpaceUrl();
            if (baseUrl == null) baseUrl = "/./" + unit.getName();
            
            String separator = baseUrl.contains("?") ? "&" : "?";
            String clusteredUrl = baseUrl + separator + "total_members=" + totalMembers + "&id=" + (instanceId + 1) + "&backup_id=" + backupId;
            
            // Create a copy of the unit with the specialized URL
            ServiceUnit specializedUnit = new ServiceUnit(unit.getName(), unit.getServices(), unit.getSla(), unit.hasEmbeddedSpace());
            specializedUnit.setSpaceUrl(clusteredUrl);
            dsc.deploy(specializedUnit);
        } else {
            dsc.deploy(unit);
        }
    }

    @Override
    public void undeploy(String unitName) throws RemoteException {
        System.out.println("[DSM] Undeploying ServiceUnit: " + unitName);
        // In a real implementation, we would need to know which DSC has this unit.
        // For now, let's try to undeploy from all known DSCs.
        List<DistributedServiceContainer> dscs = findAllDSCs();
        for (DistributedServiceContainer dsc : dscs) {
            dsc.undeploy(unitName);
        }
    }

    @Override
    public void redeploy(ServiceUnit unit) throws RemoteException {
        System.out.println("[DSM] Redeploying ServiceUnit: " + unit.getName());
        undeploy(unit.getName());
        deploy(unit);
    }

    private int currentDscIndex = 0;

    private synchronized DistributedServiceContainer findAvailableDSC() throws RemoteException {
        List<DistributedServiceContainer> dscs = findAllDSCs();
        if (dscs.isEmpty()) return null;
        
        // Round-robin distribution
        DistributedServiceContainer selected = dscs.get(currentDscIndex % dscs.size());
        currentDscIndex++;
        return selected;
    }

    private List<DistributedServiceContainer> findAllDSCs() throws RemoteException {
        List<DistributedServiceContainer> result = new ArrayList<>();
        String host = System.getProperty("lus.host", "localhost");
        int port = Integer.getInteger("lus.port", 1099);
        
        try {
            LookupLocator locator = new LookupLocator(host, port);
            ServiceRegistrar registrar = locator.getRegistrar();
            
            if (registrar != null) {
                ServiceTemplate tmpl = new ServiceTemplate(null, new Class[]{DistributedServiceContainer.class}, null);
                ServiceMatches matches = registrar.lookup(tmpl, 100);
                for (ServiceItem item : matches.items) {
                    if (item.service instanceof DistributedServiceContainer) {
                        result.add((DistributedServiceContainer) item.service);
                    }
                }
            }
        } catch (Exception e) {
            throw new RemoteException("Failed to find DSCs", e);
        }
        return result;
    }

    @Override
    public boolean isAlive() throws RemoteException {
        return true;
    }
}
