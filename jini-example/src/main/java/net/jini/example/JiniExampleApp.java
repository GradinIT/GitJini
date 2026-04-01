package net.jini.example;

import net.jini.core.discovery.LookupLocator;
import net.jini.core.lookup.*;
import net.jini.lookup.BasicLookupService;
import net.jini.core.entry.Entry;
import net.jini.core.entry.RoutingEntry;
import net.jini.core.export.ImportService;
import net.jini.discovery.DiscoveryService;
import net.jini.export.ServiceExporter;
import net.jini.export.ServiceImporter;
import net.jini.grid.DistributedServiceContainerImpl;
import net.jini.grid.DistributedServiceManagerImpl;
import java.rmi.RemoteException;
import java.util.UUID;

public class JiniExampleApp {

    public void startLUSAndDSM() {
        try {
            System.out.println("--- Jini LUS & DSM Combined Starting ---");
            // 1. Start LUS
            ServiceRegistrar registrar = new BasicLookupService();
            String lusHost = System.getProperty("lus.host", "0.0.0.0");
            int lusPort = Integer.getInteger("lus.port", 10999);
            System.out.println("[LUS] Starting at " + lusHost + ":" + lusPort);
            DiscoveryService.register(lusHost, lusPort, registrar);
            // Also register as localhost for internal DSM to find it without network issues
            if (!"localhost".equals(lusHost) && !"127.0.0.1".equals(lusHost) && !"0.0.0.0".equals(lusHost)) {
                DiscoveryService.register("localhost", lusPort, registrar);
            } else if ("0.0.0.0".equals(lusHost)) {
                DiscoveryService.register("127.0.0.1", lusPort, registrar);
                DiscoveryService.register("localhost", lusPort, registrar);
            }
            System.out.println("[LUS] Ready.");

            // 2. Start DSM
            System.out.println("[DSM] Connecting to LUS at " + lusHost + ":" + lusPort);
            // Force the DSM to use localhost for registration in the combined process
            System.setProperty("lus.host", "localhost");
            ServiceRegistration reg = ServiceExporter.exportIfNeeded(new DistributedServiceManagerImpl());
            if (reg != null) {
                System.out.println("[DSM] Registered successfully with ID: " + reg.getServiceID());
            } else {
                System.err.println("[DSM] Failed to register: registration returned null.");
            }
            System.out.println("[DSM] Ready.");
            
            // Keep the process alive
            while (true) {
                Thread.sleep(60000);
                System.out.println("[LUS-DSM] Combined services still alive...");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startLUS() {
        try {
            System.out.println("--- Jini Lookup Service (LUS) Starting ---");
            ServiceRegistrar registrar = new BasicLookupService();
            
            String lusHost = System.getProperty("lus.host", "0.0.0.0");
            int lusPort = Integer.getInteger("lus.port", 10999);
            
            System.out.println("[LUS] Starting at " + lusHost + ":" + lusPort);
            DiscoveryService.register(lusHost, lusPort, registrar);
            
            // Keep the process alive
            System.out.println("[LUS] Ready and waiting...");
            // Instead of just joining, let's keep it running with a message
            while (true) {
                Thread.sleep(60000);
                System.out.println("[LUS] Still alive...");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startDSM() {
        try {
            System.out.println("--- Jini Distributed Service Manager (DSM) Starting ---");
            String lusHost = System.getProperty("lus.host", "localhost");
            int lusPort = Integer.getInteger("lus.port", 10999);
            System.out.println("[DSM] Connecting to LUS at " + lusHost + ":" + lusPort);
            ServiceRegistration reg = ServiceExporter.exportIfNeeded(new DistributedServiceManagerImpl());
            if (reg != null) {
                System.out.println("[DSM] Registered successfully with ID: " + reg.getServiceID());
            } else {
                System.err.println("[DSM] Failed to register: registration returned null.");
            }
            System.out.println("[DSM] Ready.");
            while(true) Thread.sleep(1000);
        } catch (Exception e) {
            System.err.println("[DSM] Fatal error during startup:");
            e.printStackTrace();
        }
    }

    public void startDSC() {
        try {
            System.out.println("--- Jini Distributed Service Container (DSC) Starting ---");
            String lusHost = System.getProperty("lus.host", "localhost");
            int lusPort = Integer.getInteger("lus.port", 10999);
            System.out.println("[DSC] Connecting to LUS at " + lusHost + ":" + lusPort);
            ServiceRegistration reg = ServiceExporter.exportIfNeeded(new DistributedServiceContainerImpl());
            if (reg != null) {
                System.out.println("[DSC] Registered successfully with ID: " + reg.getServiceID());
            } else {
                System.err.println("[DSC] Failed to register: registration returned null.");
            }
            System.out.println("[DSC] Ready.");
            while(true) Thread.sleep(1000);
        } catch (Exception e) {
            System.err.println("[DSC] Fatal error during startup:");
            e.printStackTrace();
        }
    }

    public void startService() {
        try {
            System.out.println("--- Jini Service Starting ---");
            
            String lusHost = System.getProperty("lus.host", "localhost");
            int lusPort = Integer.getInteger("lus.port", 10999);
            System.out.println("[SERVICE] Connecting to LUS at " + lusHost + ":" + lusPort);

            // Create service instance
            HelloService helloService = new HelloServiceImpl("Docker-Instance");

            // Register service
            ServiceRegistration reg = ServiceExporter.exportIfNeeded(helloService);
            
            if (reg != null) {
                System.out.println("[SERVICE] Registered successfully with ID: " + reg.getServiceID());
            }

            // Keep alive
            System.out.println("[SERVICE] Ready.");
            Thread.currentThread().join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startClient() {
        try {
            System.out.println("--- Jini Client Starting ---");
            
            String lusHost = System.getProperty("lus.host", "localhost");
            int lusPort = Integer.getInteger("lus.port", 10999);
            
            System.out.println("[CLIENT] Connecting to LUS at " + lusHost + ":" + lusPort);
            LookupLocator locator = new LookupLocator(lusHost, lusPort);
            ServiceRegistrar registrar = locator.getRegistrar();
            
            if (registrar == null) {
                System.err.println("[CLIENT] Could not find LUS!");
                return;
            }

            HelloClient helloClient = new HelloClient();
            ServiceImporter.importServices(helloClient, registrar);

            System.out.println("[CLIENT] Calling service...");
            String response = helloClient.callHello("DockerKey", "DockerUser");
            System.out.println("[CLIENT] Response: " + response);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            System.out.println("--- Jini Example Started (Monolithic Mode) ---");

            // 1. Start the Lookup Service (LUS)
            System.out.println("[LUS] Starting BasicLookupService...");
            ServiceRegistrar registrar = new BasicLookupService();
            
            // Register it in the discovery service so it can be found by host/port
            String lusHost = "localhost";
            int lusPort = 10999;
            System.setProperty("lus.host", lusHost);
            System.setProperty("lus.port", String.valueOf(lusPort));
            DiscoveryService.register(lusHost, lusPort, registrar);

            // 2. Prepare multiple services to be registered
            System.out.println("[SERVICE] Creating two HelloService implementations...");
            HelloService helloService1 = new HelloServiceImpl("Instance-1");
            HelloService helloService2 = new HelloServiceImpl("Instance-2");

            // 3. Register the services with the LUS using the Exporter
            System.out.println("[SERVICE] Attempting automatic registration via @ExportedService...");
            ServiceRegistration reg1 = ServiceExporter.exportIfNeeded(helloService1);
            ServiceRegistration reg2 = ServiceExporter.exportIfNeeded(helloService2);
            
            if (reg1 != null && reg2 != null) {
                System.out.println("[SERVICE] Both instances registered successfully.");
            }

            // 4. Test @ImportService wiring in HelloClient
            System.out.println("\n[CLIENT] --- Testing @ImportService wiring in HelloClient ---");
            HelloClient helloClient = new HelloClient();
            ServiceImporter.importServices(helloClient, registrar);

            System.out.println("[CLIENT] Calling with 'Jocke'...");
            helloClient.callHello("Jocke", "World");
            System.out.println("[CLIENT] Calling with 'Jocke'...");
            helloClient.callHello("Jocke", "World");
            System.out.println("[CLIENT] Calling with 'Jocke'...");
            helloClient.callHello("Jocke", "World");
            System.out.println("[CLIENT] Calling with 'AnotherKey'...");
            helloClient.callHello("AnotherKey", "World");
            
            System.out.println("[CLIENT] Calling with 'SomethingElse'...");
            helloClient.callHello("SomethingElse", "World");

            System.out.println("--- Jini Example Completed Successfully ---");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void checkDocker() {
        boolean inDocker = new java.io.File("/.dockerenv").exists();
        if (!inDocker) {
            // Check if we are running inside an OCI container by other means
            String cgroup = null;
            try {
                cgroup = java.nio.file.Files.readString(java.nio.file.Paths.get("/proc/1/cgroup"));
            } catch (Exception ignored) {}
            
            if (cgroup != null && (cgroup.contains("docker") || cgroup.contains("containerd") || cgroup.contains("kubepods")) || new java.io.File("/run/.containerenv").exists()) {
                System.out.println("[DOCKER] Container detected via alternate means.");
            } else {
                System.out.println("[WARNING] Not running in a Docker container (/.dockerenv not found). Continuing anyway.");
            }
        } else {
            System.out.println("[DOCKER] Running in a Docker container.");
        }
    }

    public static void main(String[] args) {
        if (args.length > 0) {
            checkDocker();
            String command = args[0].toLowerCase();
            JiniExampleApp app = new JiniExampleApp();
            switch (command) {
                case "lus":
                    app.startLUS();
                    break;
                case "lus-dsm":
                    app.startLUSAndDSM();
                    break;
                case "dsm":
                    app.startDSM();
                    break;
                case "dsc":
                    app.startDSC();
                    break;
                case "service":
                    app.startService();
                    break;
                case "client":
                    app.startClient();
                    break;
                default:
                    System.out.println("Unknown command: " + command);
                    System.out.println("Usage: java -jar jini-example.jar [lus|dsm|dsc|service|client]");
            }
        } else {
            new JiniExampleApp().run();
        }
    }
}
