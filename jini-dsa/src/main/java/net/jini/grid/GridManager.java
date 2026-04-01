package net.jini.grid;

import net.jini.core.discovery.LookupLocator;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceMatches;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.lookup.ServiceTemplate;
import java.util.ArrayList;
import java.util.List;

public class GridManager {
    public static void main(String[] args) {
        try {
            // Find all DSAs
            List<DistributedServiceAgent> dsas = findAllDSAs();
            if (dsas.isEmpty()) {
                System.out.println("No DSAs found in the grid. Make sure they are started on each node.");
                return;
            }
            System.out.println("Found " + dsas.size() + " DSAs.");

            // Basic Deployment Recommendation:
            // 2 Lookup Services, 2 DSMs, 2 DSCs per machine.
            // For simplicity, we assign roles to DSAs.
            
            // Start LUS on first 2 DSAs
            for (int i = 0; i < Math.min(2, dsas.size()); i++) {
                System.out.println("Instructing DSA " + i + " to start LUS.");
                dsas.get(i).startLUS();
            }

            // Start DSM on first 2 DSAs
            for (int i = 0; i < Math.min(2, dsas.size()); i++) {
                System.out.println("Instructing DSA " + i + " to start DSM.");
                dsas.get(i).startDSM();
            }

            // Start 2 DSCs on each DSA
            for (int i = 0; i < dsas.size(); i++) {
                System.out.println("Instructing DSA " + i + " to start 2 DSCs.");
                dsas.get(i).startDSC();
                dsas.get(i).startDSC();
            }
            
            System.out.println("Basic grid infrastructure started successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<DistributedServiceAgent> findAllDSAs() throws Exception {
        List<DistributedServiceAgent> result = new ArrayList<>();
        String host = System.getProperty("lus.host", "localhost");
        int port = Integer.getInteger("lus.port", 10999);
        
        LookupLocator locator = new LookupLocator(host, port);
        ServiceRegistrar registrar = locator.getRegistrar();
        
        if (registrar != null) {
            ServiceTemplate tmpl = new ServiceTemplate(null, new Class[]{DistributedServiceAgent.class}, null);
            ServiceMatches matches = registrar.lookup(tmpl, 100);
            for (ServiceItem item : matches.items) {
                if (item.service instanceof DistributedServiceAgent) {
                    result.add((DistributedServiceAgent) item.service);
                }
            }
        }
        return result;
    }
}
