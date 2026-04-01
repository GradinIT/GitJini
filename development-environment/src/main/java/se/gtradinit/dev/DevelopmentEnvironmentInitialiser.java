package se.gtradinit.dev;

import net.jini.grid.DistributedServiceManager;
import net.jini.grid.ServiceUnit;
import net.jini.grid.ServiceUnitLoader;
import net.jini.core.discovery.LookupLocator;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceMatches;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.lookup.ServiceTemplate;

import java.io.*;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class DevelopmentEnvironmentInitialiser {

    public static void main(String[] args) {
        System.setProperty("skip.docker", "false");
        try {
            DevelopmentEnvironmentInitialiser initialiser = new DevelopmentEnvironmentInitialiser();
            initialiser.init();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private ScheduledExecutorService healthCheckExecutor;
    private Set<String> expectedContainerIds = new HashSet<>();

    public void init() throws Exception {
        System.out.println("Starting Development Environment Initialiser...");

        if (!Boolean.getBoolean("skip.docker")) {
            // Start Docker environment
            ensureDockerRunning();
            cleanupOrphans();
            provisionContainers();
            startHealthCheck();
        } else {
            System.out.println("Skipping Docker environment setup as 'skip.docker' is set to true.");
        }

        // 1. & 2. Reading files from resources
        InputStream slaStream = getClass().getResourceAsStream("/SLA.xml");
        InputStream puStream = getClass().getResourceAsStream("/testsu.xml");

        if (slaStream == null || puStream == null) {
            throw new FileNotFoundException("Required resource files SLA.xml or testsu.xml not found!");
        }

        byte[] slaBytes = slaStream.readAllBytes();
        byte[] puBytes = puStream.readAllBytes();

        // 3. Creating test-su.jar
        File jarFile = new File("test-su.jar");
        createJar(jarFile, slaBytes, puBytes);
        System.out.println("Created " + jarFile.getAbsolutePath());

        if (!Boolean.getBoolean("skip.docker")) {
            // Wait a bit for LUS/DSM to be available in the newly started containers
            System.out.println("Waiting for grid services to start...");
            Thread.sleep(15000); // 15 seconds
        }

        // 4. Deploying the jar file
        try {
            deployJar(jarFile);
        } catch (Exception e) {
            System.err.println("Deployment failed: " + e.getMessage());
        }
    }

    private void ensureDockerRunning() throws IOException, InterruptedException {
        System.out.println("Checking if Docker daemon is running...");
        Process process = Runtime.getRuntime().exec("docker info");
        if (process.waitFor() != 0) {
            System.out.println("Docker daemon is not running. Attempting to start it...");
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("mac")) {
                Runtime.getRuntime().exec("open -a Docker");
            } else if (os.contains("linux")) {
                Runtime.getRuntime().exec("sudo systemctl start docker");
            } else {
                throw new IOException("Unsupported OS for automatic Docker start: " + os);
            }

            // Wait for Docker to start
            int attempts = 0;
            while (attempts < 12) { // Wait up to 60 seconds
                Thread.sleep(5000);
                process = Runtime.getRuntime().exec("docker info");
                if (process.waitFor() == 0) {
                    System.out.println("Docker daemon started.");
                    return;
                }
                attempts++;
                System.out.println("Still waiting for Docker daemon...");
            }
            throw new IOException("Failed to start Docker daemon after 60 seconds.");
        }
        System.out.println("Docker daemon is running.");
    }

    private void cleanupOrphans() throws IOException, InterruptedException {
        System.out.println("Cleaning up orphan containers...");
        String[] command = {"docker", "compose", "-f", PATH, "down", "--remove-orphans"};
        executeCommand(command);
    }
    private final String PATH = "development-environment/development-docker-compose.yml";
    private void provisionContainers() throws IOException, InterruptedException {
        System.out.println("Provisioning Docker containers...");
        String[] command = {"docker", "compose", "-f", PATH, "up", "-d"};
        executeCommand(command);

        // Verify that containers are actually running
        System.out.println("Verifying container status...");
        ProcessBuilder pb = new ProcessBuilder("docker", "compose", "-f", PATH, "ps", "--format", "json");
        Process process = pb.start();
        boolean anyExited = false;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[DOCKER STATUS] " + line);
                if (line.toLowerCase().contains("\"state\":\"exited\"") || line.toLowerCase().contains("\"status\":\"exited\"")) {
                    anyExited = true;
                }
            }
        }
        if (process.waitFor() != 0) {
            System.err.println("Warning: 'docker compose ps' failed.");
        }

        if (anyExited) {
            System.err.println("CRITICAL: Some containers have exited unexpectedly! Checking logs...");
            String[] logsCommand = {"docker", "compose", "-f", PATH, "logs", "--tail", "50"};
            ProcessBuilder logsPb = new ProcessBuilder(logsCommand);
            logsPb.inheritIO();
            logsPb.start().waitFor();
        } else {
            System.out.println("Containers appear to be running. Checking if they stay alive for 10 seconds...");
            Thread.sleep(10000);
            ProcessBuilder pbCheck = new ProcessBuilder("docker", "compose", "-f", PATH, "ps", "--format", "json");
            Process pCheck = pbCheck.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(pCheck.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[DOCKER STATUS CHECK] " + line);
                    if (line.toLowerCase().contains("\"state\":\"exited\"") || line.toLowerCase().contains("\"status\":\"exited\"")) {
                        anyExited = true;
                    }
                }
            }
            if (anyExited) {
                System.err.println("CRITICAL: Some containers exited after 10 seconds! Checking logs...");
                String[] logsCommand = {"docker", "compose", "-f", PATH, "logs", "--tail", "50"};
                ProcessBuilder logsPb = new ProcessBuilder(logsCommand);
                logsPb.inheritIO();
                logsPb.start().waitFor();
            }
        }
    }

    private void executeCommand(String[] command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.inheritIO();
        Process process = pb.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Command failed with exit code: " + exitCode);
        }
    }

    private void startHealthCheck() {
        System.out.println("Starting asynchronous Docker health check (every 5 seconds)...");
        
        // Initialize expected container IDs if not already done
        try {
            captureExpectedContainers();
        } catch (Exception e) {
            System.err.println("Failed to capture expected containers for health check: " + e.getMessage());
        }

        healthCheckExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "DockerHealthCheckThread");
            t.setDaemon(true);
            return t;
        });

        healthCheckExecutor.scheduleAtFixedRate(() -> {
            try {
                if (!isDockerHealthy()) {
                    System.err.println("CRITICAL: Docker environment is unhealthy! Shutting down...");
                    shutdown();
                }
            } catch (Exception e) {
                System.err.println("Error during health check: " + e.getMessage());
            }
        }, 1, 5, TimeUnit.SECONDS);
    }

    private void captureExpectedContainers() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("docker", "compose", "-f", PATH, "ps", "--format", "json");
        Process process = pb.start();
        expectedContainerIds.clear();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Assuming the JSON format has an "ID" or "Name" field. "ID" is safer.
                // docker compose ps --format json output typically looks like: {"ID":"...", "Name":"...", "State":"...", ...}
                String id = extractValue(line, "ID");
                if (id != null && !id.isEmpty()) {
                    expectedContainerIds.add(id);
                }
            }
        }
        process.waitFor();
        System.out.println("Captured " + expectedContainerIds.size() + " expected containers for health monitoring.");
    }

    private String extractValue(String json, String key) {
        String searchKey = "\"" + key + "\":\"";
        int start = json.indexOf(searchKey);
        if (start != -1) {
            start += searchKey.length();
            int end = json.indexOf("\"", start);
            if (end != -1) {
                return json.substring(start, end);
            }
        }
        return null;
    }

    private boolean isDockerHealthy() throws IOException, InterruptedException {
        System.out.println("Checking Docker environment health...");
        ProcessBuilder pb = new ProcessBuilder("docker", "compose", "-f", PATH, "ps", "--format", "json");
        Process process = pb.start();
        Set<String> runningContainerIds = new HashSet<>();
        boolean anyExited = false;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String id = extractValue(line, "ID");
                String state = extractValue(line, "State");
                if (state == null) state = extractValue(line, "Status");

                if (id != null) {
                    if ("running".equalsIgnoreCase(state) || "up".equalsIgnoreCase(state) || (state != null && state.toLowerCase().contains("up"))) {
                        runningContainerIds.add(id);
                    } else if (line.toLowerCase().contains("\"state\":\"exited\"") || line.toLowerCase().contains("\"status\":\"exited\"")) {
                        anyExited = true;
                    }
                }
            }
        }
        process.waitFor();

        if (anyExited) {
            System.err.println("Health check failed: One or more containers have exited.");
            return false;
        }

        if (runningContainerIds.size() != expectedContainerIds.size()) {
            System.err.println("Health check failed: Expected " + expectedContainerIds.size() + " containers, but found " + runningContainerIds.size() + " running.");
            return false;
        }

        for (String id : expectedContainerIds) {
            if (!runningContainerIds.contains(id)) {
                System.err.println("Health check failed: Expected container " + id + " is not running.");
                return false;
            }
        }

        return true;
    }

    private void shutdown() {
        System.out.println("Shutdown initiated...");
        if (healthCheckExecutor != null) {
            healthCheckExecutor.shutdownNow();
        }
        // Force exit as requested by the user callback requirement
        System.exit(1);
    }

    private void createJar(File jarFile, byte[] slaBytes, byte[] puBytes) throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile), manifest)) {
            // Put SLA.xml as META-INF/spring/sla.xml
            JarEntry slaEntry = new JarEntry("META-INF/spring/sla.xml");
            jos.putNextEntry(slaEntry);
            jos.write(slaBytes);
            jos.closeEntry();

            // Put testsu.xml as META-INF/spring/pu.xml
            JarEntry puEntry = new JarEntry("META-INF/spring/pu.xml");
            jos.putNextEntry(puEntry);
            jos.write(puBytes);
            jos.closeEntry();
        }
    }

    private void deployJar(File jarFile) throws Exception {
        System.out.println("Attempting to deploy " + jarFile.getName() + " to the grid...");

        // Find DSM
        DistributedServiceManager dsm = findDSM();
        if (dsm == null) {
            throw new Exception("Distributed Service Manager (DSM) not found! Is the grid running?");
        }

        // Load ServiceUnit from JAR
        ServiceUnit unit = ServiceUnitLoader.load(jarFile);
        
        // Deploy via DSM
        dsm.deploy(unit);
        System.out.println("Deployment command sent to DSM for unit: " + unit.getName());
    }

    private DistributedServiceManager findDSM() throws Exception {
        boolean skipDocker = Boolean.getBoolean("skip.docker");
        String host = System.getProperty("lus.host", "localhost");
        int port = Integer.getInteger("lus.port", 10999);

        System.out.println("Connecting to LUS at " + host + ":" + port + " to find DSM...");
        
        int maxAttempts = skipDocker ? 1 : 20;
        int delay = 5000; // 5 seconds
        
        for (int i = 0; i < maxAttempts; i++) {
            try {
                LookupLocator locator = new LookupLocator(host, port);
                ServiceRegistrar registrar = locator.getRegistrar();

                if (registrar != null) {
                    System.out.println("LUS found, looking for DSM...");
                    ServiceTemplate tmpl = new ServiceTemplate(null, new Class[]{DistributedServiceManager.class}, null);
                    ServiceMatches matches = registrar.lookup(tmpl, 1);
                    if (matches != null && matches.totalMatches > 0) {
                        DistributedServiceManager dsm = (DistributedServiceManager) matches.items[0].service;
                        System.out.println("DSM found! ID: " + matches.items[0].serviceID);
                        return dsm;
                    }
                    System.out.println("LUS connected, but DSM not yet registered. Searching for all services to debug...");
                    ServiceMatches all = registrar.lookup(new ServiceTemplate(null, null, null), 100);
                    if (all != null) {
                        System.out.println("Total services registered in LUS: " + all.totalMatches);
                        if (all.items != null) {
                            for (ServiceItem item : all.items) {
                                System.out.println(" - Service ID: " + item.serviceID + ", Class: " + (item.service != null ? item.service.getClass().getName() : "null"));
                            }
                        }
                    } else {
                        System.out.println("LUS connected, but lookup(all) returned null.");
                    }
                } else {
                    System.out.println("LUS not yet available at " + host + ":" + port);
                }
                if (maxAttempts > 1) {
                    System.out.println("DSM not found in LUS, retrying in " + (delay / 1000) + " seconds... (Attempt " + (i + 1) + "/" + maxAttempts + ")");
                }
            } catch (Exception e) {
                if (maxAttempts > 1) {
                    System.err.println("Error connecting to LUS: " + e.getMessage() + ", retrying in " + (delay / 1000) + " seconds... (Attempt " + (i + 1) + "/" + maxAttempts + ")");
                } else if (!"localhost".equals(host)) {
                    System.err.println("Error connecting to LUS: " + e.getMessage());
                }
            }
            if (i < maxAttempts - 1) {
                Thread.sleep(delay);
            }
        }
        
        return null;
    }
}
