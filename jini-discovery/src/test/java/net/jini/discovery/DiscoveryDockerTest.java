package net.jini.discovery;

import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.lookup.ServiceTemplate;
import org.junit.jupiter.api.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class DiscoveryDockerTest {

    private static final String DOCKER_COMPOSE_PATH = "jini-discovery/src/test/resources/docker/docker-compose.test.yml";
    private static final int LUS_PORT = 11099;

    @BeforeAll
    public static void setupDocker() throws Exception {
        if (System.getProperty("skip.docker") != null && Boolean.getBoolean("skip.docker")) {
            Assumptions.assumeTrue(false, "Skipping Docker tests");
        }

        // Check if docker is running
        boolean dockerRunning = false;
        try {
            Process check = Runtime.getRuntime().exec(new String[]{"/usr/local/bin/docker", "info"});
            if (check.waitFor() == 0) dockerRunning = true;
        } catch (IOException e) {
            try {
                Process check = Runtime.getRuntime().exec(new String[]{"docker", "info"});
                if (check.waitFor() == 0) dockerRunning = true;
            } catch (IOException e2) {
                // ignore
            }
        }

        if (!dockerRunning) {
            Assumptions.assumeTrue(false, "Docker daemon not running");
        }

        System.out.println("Starting LUS container...");
        executeDockerCompose("up", "-d", "--build");

        // Wait for LUS to be ready
        System.out.println("Waiting for LUS to start...");
        boolean ready = false;
        for (int i = 0; i < 20; i++) {
            try {
                ServiceRegistrar registrar = DiscoveryService.getRegistrar("localhost", LUS_PORT);
                if (registrar != null) {
                    ready = true;
                    System.out.println("LUS is ready!");
                    break;
                }
            } catch (Exception e) {
                // Ignore and retry
            }
            Thread.sleep(2000);
        }
        assertTrue(ready, "LUS container failed to start in time");
    }

    @AfterAll
    public static void tearDownDocker() throws Exception {
        if (System.getProperty("skip.docker") == null || !Boolean.getBoolean("skip.docker")) {
            System.out.println("Stopping LUS container...");
            try {
                executeDockerCompose("down");
            } catch (Exception e) {
                System.err.println("Failed to stop Docker container: " + e.getMessage());
            }
        }
    }

    @Test
    public void testDiscoveryFromDocker() throws Exception {
        ServiceRegistrar registrar = DiscoveryService.getRegistrar("localhost", LUS_PORT);
        assertNotNull(registrar, "Should find the registrar from the Docker container");
        System.out.println("Successfully discovered registrar from Docker container!");
        try {
            registrar.getEntryClasses(new ServiceTemplate(null, null, null));
        }
        catch (Exception e) {
            fail("Should not throw exception");
        }
    }

    private static String getDockerComposePath() {
        File currentDir = new File(".").getAbsoluteFile();
        // If we are already in jini-discovery, we need to go up one level to find the root
        // or just check if jini-discovery exists here.
        if (new File(currentDir, "jini-discovery/src/test/resources/docker/docker-compose.test.yml").exists()) {
            return new File(currentDir, "jini-discovery/src/test/resources/docker/docker-compose.test.yml").getAbsolutePath();
        }
        
        // If we are inside jini-discovery
        if (new File(currentDir, "src/test/resources/docker/docker-compose.test.yml").exists()) {
             return new File(currentDir, "src/test/resources/docker/docker-compose.test.yml").getAbsolutePath();
        }

        // Search upwards for the root
        while (currentDir != null && !new File(currentDir, "pom.xml").exists()) {
            currentDir = currentDir.getParentFile();
        }
        if (currentDir != null && new File(currentDir, "jini-discovery/src/test/resources/docker/docker-compose.test.yml").exists()) {
            return new File(currentDir, "jini-discovery/src/test/resources/docker/docker-compose.test.yml").getAbsolutePath();
        }
        
        return "src/test/resources/docker/docker-compose.test.yml"; // Fallback
    }

    private static void executeDockerCompose(String... args) throws IOException, InterruptedException {
        String dockerComposePath = getDockerComposePath();
        File dockerComposeFile = new File(dockerComposePath);
        if (!dockerComposeFile.exists()) {
            throw new IOException("Docker compose file not found: " + dockerComposePath);
        }
        String[] command = new String[args.length + 4];
        command[0] = "/usr/local/bin/docker";
        command[1] = "compose";
        command[2] = "-f";
        command[3] = dockerComposePath;
        System.arraycopy(args, 0, command, 4, args.length);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(dockerComposeFile.getParentFile());
        pb.inheritIO();
        
        System.out.println("Executing docker-compose with config: " + dockerComposePath);
        
        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            // Fallback to "docker" if full path fails
            command[0] = "docker";
            pb = new ProcessBuilder(command);
            pb.directory(dockerComposeFile.getParentFile());
            pb.inheritIO();
            process = pb.start();
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Docker compose command failed with exit code: " + exitCode);
        }
    }
}
