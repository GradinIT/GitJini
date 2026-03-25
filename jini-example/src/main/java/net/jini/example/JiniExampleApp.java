package net.jini.example;

import net.jini.core.lookup.*;
import net.jini.lookup.BasicLookupService;
import net.jini.core.entry.Entry;
import net.jini.core.entry.RoutingEntry;
import net.jini.core.export.ImportService;
import net.jini.discovery.DiscoveryService;
import net.jini.export.ServiceExporter;
import net.jini.export.ServiceImporter;
import java.rmi.RemoteException;
import java.util.UUID;

public class JiniExampleApp {

    public void run() {
        try {
            System.out.println("--- Jini Example Started ---");

            // 1. Start the Lookup Service (LUS)
            System.out.println("[LUS] Starting BasicLookupService...");
            ServiceRegistrar registrar = new BasicLookupService();
            
            // Register it in the discovery service so it can be found by host/port
            String lusHost = "localhost";
            int lusPort = 1099;
            System.setProperty("lus.host", lusHost);
            System.setProperty("lus.port", String.valueOf(lusPort));
            DiscoveryService.register(lusHost, lusPort, registrar);

            // 2. Prepare multiple services to be registered
            System.out.println("[SERVICE] Creating two HelloService implementations...");
            HelloService helloService1 = new HelloServiceImpl("Instance-1");
            HelloService helloService2 = new HelloServiceInstance2("Instance-2");

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

    public static void main(String[] args) {
        new JiniExampleApp().run();
    }
}
