package net.jini.grid;

import net.jini.core.discovery.LookupLocator;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceMatches;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.export.ServiceExporter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class DeploymentUtility {

    public static void main(String[] args) {
        if (args.length < 1) {
            printUsage();
            return;
        }

        String command = args[0].toLowerCase();
        try {
            switch (command) {
                case "dsa":
                    startDSA();
                    break;
                case "deploy":
                    if (args.length < 2) {
                        System.err.println("Usage: deploy <unitPath> [slaPath]");
                        return;
                    }
                    String slaPath = args.length > 2 ? args[2] : null;
                    deployUnit(args[1], slaPath);
                    break;
                case "undeploy":
                    if (args.length < 2) {
                        System.err.println("Usage: undeploy <unitName>");
                        return;
                    }
                    undeployUnit(args[1]);
                    break;
                case "redeploy":
                    if (args.length < 2) {
                        System.err.println("Usage: redeploy <unitName>");
                        return;
                    }
                    redeployUnit(args[1]);
                    break;
                default:
                    printUsage();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void startDSA() throws Exception {
        System.out.println("Starting Distributed Service Agent (DSA)...");
        DistributedServiceAgent dsa = new DistributedServiceAgentImpl();
        ServiceExporter.exportIfNeeded(dsa);
        System.out.println("DSA is running and registered.");
        
        // Keep process alive
        Thread.currentThread().join();
    }

    private static void deployUnit(String unitPath, String slaPath) throws Exception {
        System.out.println("Initiating deployment for unit path: " + unitPath);

        DistributedServiceManager dsm = findDSM();
        if (dsm == null) {
            System.err.println("Error: Could not find Distributed Service Manager (DSM) in the grid.");
            return;
        }

        File unitFile = new File(unitPath);
        ServiceUnit unit = ServiceUnitLoader.load(unitFile);

        // Optional SLA override path
        if (slaPath != null) {
            File slaFile = new File(slaPath);
            if (slaFile.exists()) {
                SLA override = loadSLA(slaPath);
                unit = new ServiceUnit(unit.getName(), unit.getServices(), override, unit.hasEmbeddedSpace());
                System.out.println("Loaded SLA override from: " + slaPath + " -> " + override);
            } else {
                System.out.println("SLA override file not found at: " + slaPath + ". Using SLA from Service Unit or default.");
            }
        }

        dsm.deploy(unit);
        System.out.println("Deployment command sent to DSM.");
    }

    static SLA loadSLA(String slaPath) throws Exception {
        File xmlFile = new File(slaPath);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setNamespaceAware(true);
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(xmlFile);
        doc.getDocumentElement().normalize();

        SLA sla = new SLA();
        NodeList nList = doc.getElementsByTagNameNS("http://www.openspaces.org/schema/sla", "sla");
        if (nList.getLength() > 0) {
            Element element = (Element) nList.item(0);
            if (element.hasAttribute("cluster-schema")) {
                sla.setClusterSchema(element.getAttribute("cluster-schema"));
            }
            if (element.hasAttribute("number-of-instances")) {
                sla.setNumberOfInstances(Integer.parseInt(element.getAttribute("number-of-instances")));
            }
            if (element.hasAttribute("number-of-backups")) {
                sla.setNumberOfBackups(Integer.parseInt(element.getAttribute("number-of-backups")));
            }
            if (element.hasAttribute("max-instances-per-vm")) {
                sla.setMaxInstancesPerVM(Integer.parseInt(element.getAttribute("max-instances-per-vm")));
            }
        }
        return sla;
    }

    private static void undeployUnit(String unitName) throws Exception {
        System.out.println("Initiating undeployment for unit: " + unitName);
        
        DistributedServiceManager dsm = findDSM();
        if (dsm == null) {
            System.err.println("Error: Could not find Distributed Service Manager (DSM) in the grid.");
            return;
        }
        
        dsm.undeploy(unitName);
        System.out.println("Undeployment command sent to DSM.");
    }

    private static void redeployUnit(String unitName) throws Exception {
        System.out.println("Initiating redeployment for unit: " + unitName);
        
        DistributedServiceManager dsm = findDSM();
        if (dsm == null) {
            System.err.println("Error: Could not find Distributed Service Manager (DSM) in the grid.");
            return;
        }
        
        ServiceUnit unit = new ServiceUnit(unitName, new ArrayList<>());
        dsm.redeploy(unit);
        System.out.println("Redeployment command sent to DSM.");
    }

    private static DistributedServiceManager findDSM() throws Exception {
        String host = System.getProperty("lus.host", "localhost");
        int port = Integer.getInteger("lus.port", 1099);
        
        LookupLocator locator = new LookupLocator(host, port);
        ServiceRegistrar registrar = locator.getRegistrar();
        
        if (registrar != null) {
            ServiceTemplate tmpl = new ServiceTemplate(null, new Class[]{DistributedServiceManager.class}, null);
            ServiceMatches matches = registrar.lookup(tmpl, 1);
            if (matches.totalMatches > 0) {
                return (DistributedServiceManager) matches.items[0].service;
            }
        }
        return null;
    }

    private static void printUsage() {
        System.out.println("Usage: DeploymentUtility <command> [options]");
        System.out.println("Commands:");
        System.out.println("  dsa                        - Start the Distributed Service Agent on this machine");
        System.out.println("  deploy <unitPath> [sla]    - Deploy a Service Unit directory/JAR to the grid via DSM");
        System.out.println("  undeploy <unitName>        - Undeploy a Service Unit from the grid via DSM");
        System.out.println("  redeploy <unitName>        - Redeploy a Service Unit to the grid via DSM");
    }
}
